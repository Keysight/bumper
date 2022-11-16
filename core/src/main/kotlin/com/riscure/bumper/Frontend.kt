package com.riscure.bumper

import arrow.core.*
import com.riscure.Digest
import com.riscure.digest
import com.riscure.dobby.clang.*
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.Parser
import com.riscure.bumper.parser.UnitState
import com.riscure.bumper.preprocessor.Preprocessor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

typealias Result<R> = Either<Throwable, R>

/**
 * Assembles the various stages into a frontend pipeline for processing
 * translation units.
 */
class Frontend<Exp, Stmt, out S : UnitState<Exp, Stmt>>(
    private val preprocessor: Preprocessor,
    private val parser: Parser<Exp, Stmt, S>,
    private val cppStorage: Storage
) : Preprocessor by preprocessor,
    Parser<Exp, Stmt, S> by parser {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * Computes a stable cache location for a given input file with its compilation options.
     * If the (contents of) the input file change, or the compilation options change,
     * this location also changes.
     */
    fun preprocessedAt(main: File, command: Options): Result<File> = Either.catch {
        val digest = main.digest().plus(command.digest())
        cppStorage.inputAddressed(main.nameWithoutExtension, digest, suffix = ".c")
    }

    /**
     * Process a translation unit represented by the given main file.
     */
    fun process(main: File, command: Options): Result<S> =
        // compute the location of the preprocessed input.
        preprocessedAt(main, command).flatMap { cpped ->
            // If it doesn't exist yet, preprocess the input file
            // otherwise just use it as is.
            // TODO validation to avoid reusing invalid output
            val cppResult = if (!cpped.exists()) {
                preprocess(main, command, cpped)
            } else {
                log.info("Found cached preprocesser output for '$main' at '$cpped'")
                Unit.right()
            }

            cppResult.flatMap {
                // Call the parser with the preprocessed source.
                // This does not need compile arguments, because preprocessing happened already.
                // We make sure that the result will be identified by the given TUID.
                parser.parse(cpped, listOf(), TUID(main.toPath()))
            }

        }
}

fun Options.digest(): Digest =
    Digest.combineAll(map { o -> listOf(o.opt.key).plus(o.values).digest() })