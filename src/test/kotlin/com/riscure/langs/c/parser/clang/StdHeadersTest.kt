package com.riscure.langs.c.parser.clang

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrHandle
import arrow.core.some
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.pp.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

private val uint32 = Type.Named("__uint32_t", Type.uint)
private val uint64 = Type.Named("__uint64_t", Type.ulong)

/**
 * In this test, we parse and pretty-print some standard headers.
 */
class StdHeadersTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(Path.of("clang"), storage)

    private fun astOnly(input: String, opts: Options = listOf(), debug: Boolean = false): TranslationUnit {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(input) } .toFile()
        file.deleteOnExit()
        return process(file, opts, debug)
    }

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
            is Either.Left -> fail("Expected successful processing, got error: ${result.value}")
            is Either.Right -> result.value.first
        }
    }

    companion object {
        // https://en.cppreference.com/w/c/header
        @JvmStatic
        fun headers(): Stream<Arguments> = Stream.of(
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

        /* Function signatures of some functions that we verify from the headers */
        @JvmStatic
        fun functions(): Stream<Arguments> = Stream.of(
            /* assert.h --------------------------------------------------------- */
            Arguments.of(
                "assert", "__assert_fail",
                Type.function(
                    Type.Void(),
                    Param("__assertion", Type.Ptr(Type.char)),
                    Param("__file", Type.Ptr(Type.char)),
                    Param("__line", Type.uint),
                    Param("__function", Type.Ptr(Type.char)),
                )
            ),

            /* stdio.h --------------------------------------------------------- */
            Arguments.of(
                "stdio", "printf",
                Type.function(
                    Type.int,
                    Param("__format", Type.Ptr(Type.char, attrs = listOf(Attr.Constant))),
                    vararg = true
                )
            )
        )

        /* Struct types that we verify from the headers */
        @JvmStatic
        fun structs(): Stream<Arguments> = Stream.of(
            /* signal.h ----------------------------------------------- */
            Arguments.of(
                "signal", "_fpx_sw_bytes",
                listOf(
                    Field("magic1", uint32),
                    Field("extended_size", uint32),
                    Field("xstate_bv", uint64),
                    Field("xstate_size", uint32),
                    Field("__glibc_reserved1", Type.array(uint32, 7L.some()))
                ),
            )
        )
    }

    /**
     * We parse and print the top-level entities of the headers, and parse them again.
     * The two parse results should be equal, modulo metadata.
     * This is an "internal consistency" test.
     * I.e., if the parser always yields the empty TranslationUnit, this succeeds.
     */
    @ParameterizedTest(name = "roundtrip {0}.h")
    @MethodSource("headers")
    fun testStdHeaders(header: String) {
        val input = """
            #include <$header.h>
        """.trimIndent()

        println("Roundtrip test for: $input")

        val (ast1, pp1) = astOf(input)
        val (ast2, pp2) = try {
            astOf(pp1)
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

    @ParameterizedTest(name = "{0}.h:{1} function type")
    @MethodSource("functions")
    fun testStdFunction(header: String, ident: String, type: Type) {
        val (ast, _) = astOf("""
            #include <$header.h>
        """.trimIndent())

        val target = assertNotNull(ast.functions.find { it.name == ident })
        assertEquals(type, target.type())
    }

    @ParameterizedTest(name = "{0}.h:{1} struct declaration ")
    @MethodSource("structs")
    fun testStdStructs(header: String, ident: String, fields: List<Field>) {
        val ast = astOnly("""
            #include <$header.h>
        """.trimIndent())

        val target = assertNotNull(ast.structs.find { it.name == ident })
        assertEquals(fields, target.fields)
    }
}