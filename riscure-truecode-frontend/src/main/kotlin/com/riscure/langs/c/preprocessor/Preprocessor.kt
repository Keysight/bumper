package com.riscure.langs.c.preprocessor

import arrow.core.*
import com.riscure.dobby.clang.Options
import java.io.File
import java.io.Reader
import java.io.Writer
import kotlin.io.path.deleteIfExists
import kotlin.io.path.forEachLine
import kotlin.io.path.writer

interface Preprocessor {
    class Error(input: File, reason: List<String>):
        Throwable("C preprocessor failed while processing ${input}, saying:\n${reason.joinToString { it }}"){
            constructor(input: File, reason: String) : this(input, listOf(reason))
        }

    /**
     * Preprocess a source file using the given list of CPP options.
     */
    fun preprocess(main: File, opts: Options, out: File): Either<Throwable, Unit>

}