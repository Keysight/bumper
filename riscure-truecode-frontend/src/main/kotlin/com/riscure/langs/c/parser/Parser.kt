package com.riscure.langs.c.parser

import arrow.core.*
import java.io.File

typealias Result<T> = Either<String, T>

fun interface Parser<S : UnitState> {
    /**
     * Given a file, parse it.
     */
    fun parse(file: File) : Result<S>
}
