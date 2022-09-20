package com.riscure.langs.c

import kotlin.io.*
import arrow.core.*
import com.riscure.tc.codeanalysis.clang.compiler2.model.compilecommand.StructuredCompileCommand
import com.riscure.tc.codeanalysis.clang.compiler2.model.compilecommand.flag.CompilationFlag
import java.io.File

typealias Result<T> = Either<String, T>

fun interface Parser {

    /**
     * Parse a (preprocssed) C file into a Java AST.
     */
    fun parse(file: File): Result<TranslationUnit>
}
