package com.riscure.langs.c

import arrow.core.*
import com.riscure.dobby.clang.*
import com.riscure.langs.c.parser.Parser
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.ClangParser
import com.riscure.langs.c.parser.clang.ClangPreprocessor
import com.riscure.langs.c.parser.clang.ClangUnitState
import com.riscure.langs.c.preprocessor.Preprocessor
import java.io.File
import java.nio.file.Path

typealias Result<R> = Either<String, R>

/**
 * Assembles the various stages into a frontend pipeline for processing
 * translation units.
 */
class Frontend<S : UnitState,R>(
    val preprocessor: Preprocessor,
    val parser: Parser<S>,
    val analyzer: StaticAnalyzer<R>
) : Preprocessor by preprocessor,
    Parser<S> by parser,
    StaticAnalyzer<R> by analyzer {

    /**
     * Process a translation unit represented by the given main file.
     */
    fun process(main: File, command: Options): Result<R> {
        // call the preprocessor; this outputs a temporary file.
        val source = preprocessor.preprocess(main, command)

        // Call the parser with the preprocessed source.
        // This does not need compile arguments, because preprocessing
        // happened already.
        return parser
            .parse(File("TODO"))
            .flatMap {
                // We immediately consume the parse unit state.
                // The analyzer is in charge of extracting every bit
                // of info from it that we need, because after
                // this block it will have been "used up" (i.e., autoclosed).
                it.use { unit ->
                    analyzer.analyze(unit)
                }
            }
    }

    companion object {
        /**
         * Create a pipeline based on clang tooling.
         */
        fun clang(clang: Path) = Frontend<ClangUnitState, Void>(
            ClangPreprocessor(clang),
            ClangParser(),
            TODO()
        )
    }
}