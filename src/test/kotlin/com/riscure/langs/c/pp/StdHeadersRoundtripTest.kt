package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.ClangParser
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.*
import java.io.StringWriter
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.stream.Stream
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

    private fun astOf(input: String, opts: Options = listOf(), debug: Boolean = false): Pair<TranslationUnit, String> {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(input) } .toFile()
        file.deleteOnExit()
        val ast = process(file, opts, debug)

        val cpped = frontend.preprocessedAt(file, opts).getOrHandle { fail(it.message) }
        val extractor = Extractor(cpped, Charset.defaultCharset())
        fun bodyPrinter(tl : TopLevel) =
            extractor.rhsOf(tl)

        return Pair(ast, AstWriters { bodyPrinter(it) }
            .print(ast)
            .getOrHandle { throw it }
            .write())
    }
    private fun process(test: File, opts: Options = listOf(), debug: Boolean = false): TranslationUnit {
        if (debug) println(frontend.preprocessedAt(test, opts))

        val result = frontend
            .process(test, opts)
            .flatMap { it.ast().map{ ast -> Pair(ast, it) }}

        return when (result) {
            is Either.Left  -> fail("Expected successful processing, got error: ${result.value}")
            is Either.Right -> result.value.first
        }
    }

    companion object {
        // https://en.cppreference.com/w/c/header
        @JvmStatic
        fun headers(): Stream<Arguments> = Stream.of(
            "assert",
            // "complex",
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
            "stdbool",
            "stddef",
            "stdint",
            "stdio",
            "stdlib",
            "string",
            "tgmath",
            "threads",
            "time",
            "uchar",
            "wchar",
            "wctype"
        ).map { Arguments.of(it) }
    }

    fun roundtrip(input: String, debug: Boolean = false) {
        println("Roundtrip test for: ${input}")

        val (ast1, pp1) = astOf(input, debug = debug)
        val (ast2, pp2) = try {
            astOf(pp1, debug = debug)
        } catch (e: Throwable) {
            println("Failed to parse pretty-printed parsed input, which was:\n$pp1")
            throw e
        }

        ast1.decls.zip(ast2.decls) { l, r ->
            try {
                assertEquals(l, r.withMeta(l.meta))
            } catch (e : Throwable) {
                println("Pretty 1:\n" + Pretty.lhs(l))
                println("Pretty 2:\n" + Pretty.lhs(r))
                throw e
            }
        }
    }

    @ParameterizedTest
    @MethodSource("headers")
    fun testStdHeaders(header: String) {
        roundtrip("""
            #include <${header}.h>
        """.trimIndent(), true)
    }

}