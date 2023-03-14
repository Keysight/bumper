package com.riscure.bumper.preprocessor

import arrow.core.*
import com.riscure.bumper.parser.ParseError
import com.riscure.dobby.clang.Options
import java.io.File

interface Preprocessor {

    /**
     * Preprocess a source file using the given list of CPP options.
     */
    fun preprocess(main: File, opts: Options, out: File): Either<ParseError.PreprocFailed, Unit>

}
