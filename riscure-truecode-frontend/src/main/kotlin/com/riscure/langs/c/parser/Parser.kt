package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.File

typealias Result<T> = Either<String, T>

interface UnitState : Closeable {
    fun getSource(range: SourceRange): String

    fun ast(): Result<TranslationUnit>
}

interface Parser<S : UnitState> {
    fun parse(file: File) : Result<S>
}
