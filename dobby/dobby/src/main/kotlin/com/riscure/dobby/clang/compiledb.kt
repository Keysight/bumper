@file:OptIn(ExperimentalSerializationApi::class)

package com.riscure.dobby.clang

import java.nio.file.Path
import arrow.core.*
import arrow.core.Validated
import com.riscure.dobby.shell.Shell
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.io.InputStream
import java.io.Writer
import java.lang.IllegalArgumentException

private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

fun interface OnUnrecognizedOption {
    fun callback(entry: String, surpriseArgument: String): Boolean

    companion object {
        @JvmStatic val ignore: OnUnrecognizedOption = OnUnrecognizedOption { _, _ -> true  }
        @JvmStatic val strict: OnUnrecognizedOption = OnUnrecognizedOption { _, _ -> false }
    }
}

/**
 * This is the raw compilation database as written/read from disk.
 * It is only the intermediate value for the serialization.
 */
data class PlainCompilationDb(val entries: List<Entry>) {
    @Serializable
    data class Entry(
        /* All relative paths in the command/arguments are relative to this dir */
        val directory: String,
        val file: String,
        /* Executable plus arguments */
        val arguments: List<String>? = null,
        /* Raw shell-quoted/escaped command */
        val command: String? = null,
        val output: String? = null
    )
}

/** Semantic model of a compilation database */
data class CompilationDb(val entries: List<Entry> = listOf()) {
    private val byMain: Map<Path, Entry> = entries.associateBy {
        // mainSource can be relative to the working directory according to
        // the specification of compilation databases
        it.resolvedMainSource
    }

    operator fun get(main: Path): Option<Entry> = byMain[main.normalize()].toOption()
    fun plus(vararg entry: Entry): CompilationDb  = copy(entries = entries.plus(entry))
    operator fun plus(other: List<Entry>): CompilationDb  = copy(entries = entries.plus(other))
    operator fun plus(other: CompilationDb): CompilationDb = copy(entries = entries.plus(other.entries))

    /**
     * Apply a path mapping to the entries' resolvedMainSource, obtaining a new database.
     */
    fun remap(mapping: (p: Path) -> Path) =
        CompilationDb(
            entries.map {
                with(it) { copy(mainSource = mapping(resolvedMainSource)) }
            }
        )

    fun filter(mapping: (p: Path) -> Boolean) =
        CompilationDb(
            entries.filter {
                it -> mapping(it.mainSource)
            }
        )

    fun toJSON(defaultCompilerExe: String) =
        entries
            .map { entry ->
                PlainCompilationDb.Entry(
                    entry.workingDirectory.toString(),
                    entry.mainSource.toString(),
                    listOf(entry.executable.getOrElse { defaultCompilerExe }) + entry.command.toArguments()
                )
            }
            .let { json.encodeToString(it) }

    data class Entry(
        /**
         * Any relative [mainSource] or path argument in [command] is relative w.r.t. this directory.
         * It is unclear from the spec if this can itself be relative.
         */
        val workingDirectory: Path,

        /**
         * The main source file. This can be relative, in which case it should be resolved
         * with respect to [workingDirectory] (see [resolvedMainSource]).
         *
         * Use with caution!
         */
        val mainSource: Path,
        val options: Options,

        /** The compiler executable */
        val executable: Option<String> = none()
    ) {
        val command get() = Command(options, listOf(resolvedMainSource.toString()))

        constructor(workingDirectory: Path, mainSource: Path)
                : this(workingDirectory, mainSource, listOf(), none())

        /**
         * Construct an entry using a command. The positional arguments of the command are discarded.
         */
        constructor(workingDirectory: Path, mainSource: Path, command: Command)
                : this(workingDirectory, mainSource, command.optArgs, none())

        constructor(mainSource: Path, options: Options)
                : this(mainSource.parent, mainSource, options, none())

        /**
         * Construct an entry using a command. The positional arguments of the command are discarded.
         */
        constructor(workingDirectory: Path, mainSource: Path, command: Command, exe: Option<String>)
                : this(workingDirectory, mainSource, command.optArgs, exe)

        val resolvedMainSource: Path get() = workingDirectory.resolve(mainSource).normalize()
    }

    companion object {
        /**
         * Read a compilation database from a file
         */
        @JvmStatic
        fun read(file: File, recover: OnUnrecognizedOption): Either<Throwable,CompilationDb> =
            Either
                .catch { file.inputStream().buffered() }
                .flatMap { read(it, recover) }

        /**
         * Read a compilation database from an input stream
         *
         * @throws IOException when inputstream cannot be read
         */
        @JvmStatic
        fun read(reader: InputStream, recover: OnUnrecognizedOption): Either<SerializationException,CompilationDb> {
            val decoded = try {
                json.decodeFromStream<List<PlainCompilationDb.Entry>>(reader).right()
            }
            catch (e: SerializationException)   { e.left() }
            catch (e: IllegalArgumentException) { InvalidCdb(e).left() }

            return decoded.flatMap { entries -> analyzePlain(PlainCompilationDb(entries), recover) }
        }

        @JvmStatic
        fun analyzePlain(
            cdb: PlainCompilationDb,
            recover: OnUnrecognizedOption
        ): Either<InvalidCdbEntry, CompilationDb> =
            cdb.entries
                .map { es -> readEntry(es, recover) }
                .sequence()
                .map { CompilationDb(it) }

        @JvmStatic fun read(reader: InputStream) = read(reader, OnUnrecognizedOption.strict)
        @JvmStatic fun read(file: File) = read(file, OnUnrecognizedOption.strict)

        @JvmStatic
        fun readEntry(entry: PlainCompilationDb.Entry, recover: OnUnrecognizedOption): Either<InvalidCdbEntry, Entry> =
            // first we try to get the arguments array
            when (val args = entry.arguments.toOption()) {
                is Some -> args.value.right()
                // if that is missing, we parse the shell command into arguments.
                None ->
                    when (val plaincmd = entry.command.toOption()) {
                        is Some -> Shell
                            .line(plaincmd.value)
                            .map { it.eval() }
                            .mapLeft { InvalidCdbEntry(entry, "failed to parse shell command '$plaincmd'") }
                        None ->
                            // neither command nor arguments is specified
                            // this is illegal according to the clang compilation database reference.
                            InvalidCdbEntry(entry, "missing both command and arguments").left()
                    }
            }
            .flatMap { args ->
                val exe  = args.firstOrNone().getOrElse { "clang" }
                val opts = args.drop(1)
                ClangParser
                    .parseArguments(opts) { unrecognizedOption ->
                        recover.callback(entry.file, unrecognizedOption)
                    }
                    .mapLeft {
                        // give some context to the exception.
                        InvalidCdbEntry(entry, "failed to parse compile command '$it'")
                    }
                    .map { cmd -> Entry(Path.of(entry.directory), Path.of(entry.file), cmd, exe.some()) }
            }
    }
}

class InvalidCdb(cause: IllegalArgumentException): SerializationException(cause) {
    override val message: String
        get() = "Failed to deserialize compilation database from JSON"
}

data class InvalidCdbEntry(val entry: PlainCompilationDb.Entry, val reason: String): SerializationException() {
    override val message: String
        get() = "Invalid compilation db entry for '${entry.file}: $reason."
}
