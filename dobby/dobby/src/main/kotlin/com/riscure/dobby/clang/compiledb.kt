@file:OptIn(ExperimentalSerializationApi::class)

package com.riscure.dobby.clang

import java.nio.file.Path
import arrow.core.*
import com.riscure.dobby.shell.Shell
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.io.InputStream
import java.io.Writer

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
data class CompilationDb(val entries: List<Entry>) {
    private val byMain: Map<Path, Entry> = entries.associateBy {
        // mainSource can be relative to the working directory according to
        // the specification of compilation databases
        it.resolvedMainSource
    }

    fun get(main: Path): Option<Entry> = byMain[main.normalize()].toOption()
    fun plus(vararg entry: Entry): CompilationDb  = copy(entries = entries.plus(entry))
    fun plus(entries: List<Entry>): CompilationDb  = copy(entries = entries.plus(entries))
    fun plus(other: CompilationDb): CompilationDb = copy(entries = entries.plus(other.entries))

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

    fun toJSON() =
        entries
            .map { entry ->
                PlainCompilationDb.Entry(
                    entry.workingDirectory.toString(),
                    entry.mainSource.toString(),
                    entry.command.toArguments()
                )
            }
            .let { Json { prettyPrint = true } .encodeToString(it) }

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
        val command: Command
    ) {
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
         */
        @JvmStatic
        fun read(reader: InputStream, recover: OnUnrecognizedOption): Either<Throwable,CompilationDb> {
            val entries = try {
                Json.decodeFromStream<List<PlainCompilationDb.Entry>>(reader).right()
            } catch (e: Throwable) {
                e.left()
            }

            return entries.flatMap { es ->
                es.map { entry ->
                    // first we try to get the arguments array
                    val args = when (val args = entry.arguments.toOption()) {
                        is Some -> args.value.right()
                        // if that is missing, we parse the shell command into arguments.
                        None ->
                            when (val plaincmd = entry.command.toOption()) {
                                is Some -> Shell.line(plaincmd.value).map { it.eval() }
                                None ->
                                    // neither command nor arguments is specified
                                    // this is illegal according to the clang compilation database reference.
                                    IllegalArgumentException("Compilation database entry missing both command and arguments").left()
                            }
                    }

                    args
                        .map { it.drop(1) } // drop executable name
                        .flatMap { it ->
                            ClangParser
                                .parseArguments(it) { unrecognizedOption ->
                                    recover.callback(entry.file, unrecognizedOption)
                                }
                                .mapLeft {
                                    // give some context to the exception.
                                    IllegalArgumentException(
                                        "failed to parse compile command for main '" + entry.file + "': $it"
                                    )
                                }
                        }
                        .map { Entry(Path.of(entry.directory), Path.of(entry.file), it) }

                }
                .sequence()
                .map { CompilationDb(it) }
            }
        }

        @JvmStatic fun read(reader: InputStream) = read(reader, OnUnrecognizedOption.strict)
        @JvmStatic fun read(file: File) = read(file, OnUnrecognizedOption.strict)
    }
}