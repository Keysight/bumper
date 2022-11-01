package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.index.TUID
import java.io.File

fun interface Parser<out S : UnitState> {
    class Error(input: File, reason: String) : Throwable("Failed to parse ${input}: ${reason}")

    /**
     * Given a file, preprocess and parse it. As a side-effect, this may perform
     * static analysis of the file, so this can fail on illtyped C programs.
     *
     * Some options are removed, namely any option conflicting with -E,
     * and any option specifying output.
     */
    fun parse(file: File, opts: Options, tuid: TUID) : Either<Throwable, S>
    fun parse(file: File, opts: Options) : Either<Throwable, S> = parse(file, opts, TUID(file.toPath()))
    fun parse(file: File) : Either<Throwable, S> = parse(file, listOf())
}
