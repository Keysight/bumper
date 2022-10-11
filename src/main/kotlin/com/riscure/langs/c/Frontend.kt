package com.riscure.langs.c

import arrow.core.*
import com.riscure.dobby.clang.*
import com.riscure.langs.c.analysis.StaticAnalysis
import com.riscure.langs.c.parser.Parser
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.ClangParser
import com.riscure.langs.c.preprocessor.clang.ClangPreprocessor
import com.riscure.langs.c.preprocessor.Preprocessor
import java.io.File
import java.nio.file.Path

typealias Result<R> = Either<Throwable, R>

/**
 * Assembles the various stages into a frontend pipeline for processing
 * translation units.
 */
class Frontend<S : UnitState>(
    private val preprocessor: Preprocessor,
    private val parser: Parser<S>,
    private val cppStorage: Storage
) : Preprocessor by preprocessor,
    Parser<S> by parser {

    /**
     * Process a translation unit represented by the given main file.
     */
    fun process(main: File, command: Options): Result<UnitState> {
        val cpped = cppStorage.inputAddressed(main.nameWithoutExtension, command, suffix = ".c")
        return preprocess(main, command, cpped)
            .flatMap {
                // Call the parser with the preprocessed source.
                // This does not need compile arguments, because preprocessing
                // happened already.
                parser.parse(cpped)
            }
    }

    companion object {
        /**
         * Create a pipeline based on clang tooling.
         */
        @JvmStatic
        fun clang(clang: Path, cppStorage: Storage) = Frontend(
            ClangPreprocessor(clang),
            ClangParser(),
            cppStorage
        )
    }
}