package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.dobby.clang.Options
import java.io.File

fun interface Parser<S : UnitState> {
    class Error(input: File, reason: String) : Throwable("Failed to parse ${input}.")

    /**
     * Given a file, preprocess and parse it. As a side-effect, this may perform
     * static analysis of the file, so this can fail on illtyped C programs.
     *
     * Some options are removed, namely any option conflicting with -E,
     * and any option specifying output.
     */
    fun parse(file: File, opts: Options) : Either<Throwable, S>
    fun parse(file: File) : Either<Throwable, S> = parse(file, listOf())
}
