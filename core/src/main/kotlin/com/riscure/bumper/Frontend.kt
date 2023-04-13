package com.riscure.bumper

import arrow.core.Either
import arrow.core.flatMap
import com.riscure.Digest
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.ParseError
import com.riscure.bumper.parser.Parser
import com.riscure.bumper.parser.UnitState
import com.riscure.bumper.preprocessor.Preprocessor
import com.riscure.digest
import com.riscure.dobby.clang.CompilationDb
import com.riscure.dobby.clang.Options
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension

/**
 * Assembles the various stages into a frontend pipeline for processing
 * translation units.
 */
open class Frontend<Exp, Stmt, S : UnitState<Exp, Stmt, S>>(
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
    fun preprocessedAt(entry: CompilationDb.Entry): Path = with(entry) {
        val digest = resolvedMainSource.toString().digest().plus(command.digest())
        return cppStorage.inputAddressed(resolvedMainSource.nameWithoutExtension, digest, suffix = ".c")
    }

    fun process(cdb: CompilationDb, main: Path): Either<ParseError, S> =
        cdb[main]
            .toEither { ParseError.MissingCompileCommand(main, cdb) }
            .flatMap { process(it) }

    /**
     * Process a translation unit represented by the given main file.
     */
    fun process(entry: CompilationDb.Entry): Either<ParseError, S> {
        // compute the location of the preprocessed input.
        return preprocessedAt(entry).let { cpped ->
            // Preprocess the file.
            val cppResult = preprocess(entry, cpped.toFile())
            cppResult
                .flatMap { cppInfo ->
                    // Call the parser with the preprocessed source.
                    // We make sure that the result will be identified by the right TUID.
                    parser
                        .parse(entry.copy(mainSource = cpped))
                        .map {
                            it
                                .withCppinfo(cppinfo = cppInfo)
                                .withTUID(TUID.mk(entry))
                        }
                }
        }
    }
}

fun Options.digest(): Digest =
    Digest.combineAll(map { o -> listOf(o.opt.key).plus(o.values).digest() })