package com.riscure.langs.c.preprocessor

import com.riscure.dobby.clang.Arg
import java.io.File

interface Preprocessor {
    /**
     * Preprocess a source file using the given list of CPP
     * options. When successful, this returns a temporary file
     * where the output was written.
     */
    fun preprocess(main: File, opts: List<Arg>): Result<File>

    companion object {
        val clang = object: Preprocessor {
            override fun preprocess(main: File, opts: List<Arg>): Result<File> {
                TODO("Not yet implemented")
            }

        }
    }
}
