package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.TranslationUnit
import java.io.File

typealias Result<T> = Either<String, T>

fun interface Parser {

    /**
     * Parse a (preprocssed) C file into a Java AST.
     */
    fun parse(file: File): Result<TranslationUnit>
}
