package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.ErasedTranslationUnit
import com.riscure.langs.c.parser.clang.ClangParser
import org.opentest4j.AssertionFailedError
import java.io.File
import kotlin.io.path.writeText
import kotlin.test.fail

open class ParseTestBase {

    protected fun <E,T> Either<E,T>.assertOK(): T =
        this.getOrHandle { error ->
            if (error is Throwable)
                fail("Expected success, got error", error)
            else
                fail("Expected success, got error: $error")
        }

    protected fun <T> Option<T>.assertOK(): T =
        this.getOrElse { fail("Expected some, got none") }

    protected fun literal(program: String, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val file: File = kotlin.io.path
            .createTempFile(suffix=".c")
            .apply { writeText(program) }
            .toFile()

        try {
            parsed(file, whenOk)
        } finally {
            // Delete this when the JVM quits, so we have time to read it
            // when we pause the JVM during debug
            file.deleteOnExit()
        }
    }

    protected fun parsed(test: File, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val unit = ClangParser()
            .parse(test)
            .assertOK()

        val ast = unit.ast.assertOK()
        whenOk(ast)
    }

    protected fun invalid(program: String) = try {
        literal(program) {}

        fail("Expected parse error.")
    } catch (e: AssertionFailedError) { /* test succeeds */ }

}