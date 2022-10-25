package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.ClangParser
import java.io.File
import kotlin.test.*
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * In this test, we parse some standard headers,
 * print the top-level entities back using the pretty printer,
 * and parse them again. The two parse results should be equal,
 * modulo metadata.
 */
class StdHeadersRoundtripTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(Path.of("clang"), storage)

    private fun astOf(input: String, opts: Options = listOf()): Pair<TranslationUnit, String> {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(input) } .toFile()
        file.deleteOnExit()
        val ast = process(file, opts)

        val extractor = Extractor(file, Charset.defaultCharset())
        fun bodyPrinter(tl : TopLevel) =
            extractor.rhsOf(tl)

        return Pair(ast, AstWriters { bodyPrinter(it) }
            .print(ast)
            .getOrHandle { throw it }
            .write())
    }
    private fun process(test: File, opts: Options = listOf()): TranslationUnit {
        val result = frontend
            .process(test, opts)
            .flatMap { it.ast().map{ ast -> Pair(ast, it) }}

        return when (result) {
            is Either.Left  -> fail("Expected successful processing, got error: ${result.value}")
            is Either.Right -> result.value.first
        }
    }

    // https://en.cppreference.com/w/c/header
    val headers = listOf(
        "assert",
        "complex",
        "ctype",
        "errno",
        "fenv",
        "float",
        "inttypes",
        "iso646",
        "limits",
        "locale",
        "math",
        "setjmp",
        "signal",
        "stdalign",
        "stdarg",
        "stdatomic",
        "stdbit",
        "stdbool",
        "stdckdint",
        "stddef",
        "stdint",
        "stdio",
        "stdlib",
        "stdnotreturn",
        "string",
        "tgmath",
        "threads",
        "time",
        "uchar",
        "wchar",
        "wctype"
    )

    fun roundtrip(input: String) {
        println("Roundtrip test for: ${input}")

        val (ast1, pp1) = astOf(input)
        val (ast2, pp2) = astOf(pp1)

        ast1.decls.zip(ast2.decls) { l, r -> assertEquals(l, r.withMeta(l.meta)) }
    }

    fun testHeader(header: String) = roundtrip("""
    #include <${header}.h>
    """.trimIndent())

    @Test
    fun testStdHeaders() {
        for (header in headers) {
            testHeader(header)
        }
    }
}