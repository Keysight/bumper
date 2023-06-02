package com.riscure.bumper.preprocessor.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.riscure.bumper.parser.ParseError
import com.riscure.bumper.preprocessor.CPPInfo
import com.riscure.bumper.preprocessor.Preprocessor
import com.riscure.bumper.preprocessor.impl.CPPInfoParser.State.*
import com.riscure.dobby.clang.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * This implements the preprocessor interface by spawning a subprocess that calls clang -E.
 */
class ClangPreprocessor(private val clang: Path) : Preprocessor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun preprocess(entry: CompilationDb.Entry, out: File): Either<ParseError.PreprocFailed, CPPInfo> = runBlocking {
        with(entry) {
            if (resolvedMainSource.extension != "c") {
                // Yes...
                ParseError.PreprocFailed(
                    resolvedMainSource,
                    "Preprocessor expects input with '.c' file extension, got '$resolvedMainSource'"
                ).left()
            }

            val cmd: Command =
                Command(options, listOf(resolvedMainSource.toString()))
                    // replacing any existing output options
                    .replace(Spec.clang11, Arg(Spec.clang11["o"], listOf(out.absolutePath)))
                    // We add -O0, which really should be implied by -E, but it isn't.
                    // It seems that in clang-16 it is.
                    .plus(Arg(Spec.clang11["O0"]))
                    .plus(Arg(Spec.clang11["E"]))
                    // We add -v to get the search paths out of stderr
                    .plus(Arg(Spec.clang11["v"]))

            log.debug("Exec: $clang ${cmd.toPOSIXArguments().joinToString(separator = " ")}")

            // Preprocess the input, writing to the output
            val res = process(
                clang.toString(), *cmd.toExecArguments().toTypedArray(),

                directory = workingDirectory.toFile(),
                stdout = Redirect.SILENT,
                stderr = Redirect.CAPTURE,
            )

            // detect failures and return the parsed preprocessor output
            if (res.resultCode != 0) {
                ParseError.PreprocFailed(resolvedMainSource, res.output.joinToString(separator = "\n")).left()
            } else {
                CPPInfoParser(workingDirectory)
                    .parse(res.output)
                    .result.right()
            }
        }
    }

}

class CPPInfoParser(val workingDirectory: Path) {

    private enum class State { NotStarted, IQuote, ISystem, Ended }

    private var state = NotStarted
    val iquote  = mutableSetOf<IPath.Quote>()
    val isystem = mutableSetOf<IPath.Sys>()

    val result get(): CPPInfo = CPPInfo(IncludePath(isystem, iquote))

    private val iquoteStart  = """#include "..." search starts here:"""
    private val isystemStart = """#include <...> search starts here:"""
    private val end = """End of search list."""

    fun parse(flow: Iterable<String>): CPPInfoParser {
        flow
            .map { it.trim() }
            .onEach { line ->
                when (state) {
                    NotStarted ->
                        if (line.trim() == iquoteStart) { state = IQuote }
                    IQuote ->
                        if (line.trim() == isystemStart) { state = ISystem }
                        else { iquote.add(IPath.Quote(workingDirectory.resolve(line))) }
                    ISystem ->
                        if (line.trim() == end) { state = Ended }
                        else { isystem.add(IPath.Sys(workingDirectory.resolve(line))) }
                    Ended -> {}
                }
            }

        return this
    }
}
