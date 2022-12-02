@file:OptIn(ExperimentalSerializationApi::class)

package com.riscure.dobby.clang

import java.nio.file.Path
import arrow.core.*
import com.riscure.dobby.shell.Shell
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.InputStream

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

/* Semantic model of a compilation database */
data class CompilationDb(val entries: List<Entry>) {
    private val byMain: Map<Path, Entry> = entries.associateBy { it.mainSource }

    fun get(main: Path): Option<Entry> = byMain[main].toOption()

    /**
     * Apply a path mapping to the entries' mainSource fields.
     */
    fun remap(mapping: (p: Path) -> Path) =
        CompilationDb(
            entries.map {
                with(it) { copy(mainSource = mapping(mainSource)) }
            }
        )

    data class Entry(
        val workingDirectory: Path,
        val mainSource: Path,
        val command: Command
    )

    companion object {
        /* Read a compilation database from a file */
        @JvmStatic
        fun read(file: File): Either<Throwable,CompilationDb> =
            Either
                .catch { file.inputStream().buffered() }
                .flatMap { read(it) }

        /* Read a compilation database from an input stream */
        @JvmStatic
        fun read(reader: InputStream): Either<Throwable,CompilationDb> {
            val entries = try {
                Json.decodeFromStream<List<PlainCompilationDb.Entry>>(reader).right()
            } catch (e: Throwable) {
                e.left()
            }

            return entries.flatMap { es ->
                es.map { entry ->
                    val args = when (val args = entry.arguments.toOption()) {
                        is Some -> args.value.right()
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
                            ClangParser.parseArguments(it).mapLeft {
                                IllegalArgumentException("failed to parse entry \"" + entry.file + "\": $it")
                            }
                        }
                        .map { Entry(Path.of(entry.directory), Path.of(entry.file), it) }

                }
                .sequence()
                .map { CompilationDb(it) }
            }
        }
    }
}