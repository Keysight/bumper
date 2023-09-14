package com.riscure.bumper.preprocessor

import arrow.core.*
import com.riscure.bumper.parser.ParseError
import com.riscure.dobby.clang.CompilationDb
import com.riscure.dobby.clang.IncludePath
import com.riscure.dobby.clang.Options
import java.io.File
import java.nio.file.Path

data class CPPInfo(
    val includepath: IncludePath = IncludePath()
)

interface Preprocessor {

    /**
     * Preprocess a source file using the given list of CPP options.
     */
    fun preprocess(entry: CompilationDb.Entry, out: File): Either<ParseError.PreprocFailed, CPPInfo>

}
