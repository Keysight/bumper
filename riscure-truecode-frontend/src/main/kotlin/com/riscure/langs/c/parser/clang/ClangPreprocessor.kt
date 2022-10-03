package com.riscure.langs.c.parser.clang

import com.riscure.dobby.clang.Arg
import com.riscure.langs.c.preprocessor.Preprocessor
import java.io.File
import java.nio.file.Path

/**
 * This implements the preprocessor interface by spawning a subprocess
 * that calls Clang.
 */
class ClangPreprocessor(val clang: Path) : Preprocessor {
    override fun preprocess(main: File, opts: List<Arg>): Result<File> {
        TODO("Not yet implemented")
    }

}