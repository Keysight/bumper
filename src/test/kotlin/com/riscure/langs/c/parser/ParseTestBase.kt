package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.ast.Declaration
import com.riscure.langs.c.ast.ErasedTranslationUnit
import com.riscure.langs.c.parser.clang.*
import com.riscure.langs.c.pp.AstWriters
import com.riscure.langs.c.pp.Extractor
import org.bytedeco.llvm.clang.CXCursor
import org.opentest4j.AssertionFailedError
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.fail

open class ParseTestBase {

    fun <E, T> Either<E, T>.assertOK(): T =
        this.getOrHandle { error ->
            if (error is Throwable)
                fail("Expected success, got error", error)
            else
                fail("Expected success, got error: $error")
        }

    fun <T> Option<T>.assertOK(): T =
        this.getOrElse { fail("Expected some, got none") }

    private fun withTemp(program: String, withFile: (file: File) -> Unit) {
        val file: File = kotlin.io.path
            .createTempFile(suffix = ".c")
            .apply { writeText(program) }
            .toFile()

        try {
            withFile(file)
        } finally {
            // Delete this when the JVM quits, so we have time to read it
            // when we pause the JVM during debug
            file.deleteOnExit()
        }
    }

    fun parsed(program: String, whenOk: (ast: ErasedTranslationUnit) -> Unit) = withTemp(program) { file ->
        parsed(file, whenOk)
    }

    fun parsed(test: File, whenOk: (ast: ErasedTranslationUnit) -> Unit) {
        val unit = ClangParser()
            .parse(test)
            .assertOK()

        val ast = unit.ast.assertOK()
        whenOk(ast)
    }

    fun invalid(program: String) = try {
        parsed(program) {}

        fail("Expected parse error.")
    } catch (e: AssertionFailedError) { /* test succeeds */
    }

    // Some temporary storage
    val storage = Storage
        .temporary("test-storage")
        .getOrHandle { throw it }

    // initiate the frontend
    val frontend = Frontend
        .clang(Path.of("clang"), storage)

    fun bumped(
        program: String,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, unit: ClangUnitState) -> Unit
    ): Unit = withTemp(program) { file -> bumped(file, opts, whenOk) }

    fun bumped(
        file: File,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, unit: ClangUnitState) -> Unit
    ) {
        println("Preprocessed input at: ${frontend.preprocessedAt(file, opts)}")

        val result = frontend
            .process(file, opts)
            .flatMap { it.ast.map { ast -> Pair(ast, it) } }
            .assertOK()

        whenOk(result.first, result.second)
    }

}