package com.riscure.langs.c.parser

import com.riscure.langs.c.Frontend
import com.riscure.langs.c.pp.Pretty
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

/*
private val uint32 = Type.Typedeffed("__uint32_t")
private val uint64 = Type.Typedeffed("__uint64_t")
*/

/**
 * In this test, we parse and pretty-print some standard headers.
 */
interface StdHeadersTest<E,S,U:UnitState<E,S>> : ParseTestBase<E, S, U> {

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
            "stdbool",
            "stddef",
            "stdint",
            "stdio",
            "stdlib",
            "string",
            // "tgmath", this depends on clang's support for the overloaded pragma, which we can't parse easily.
            "threads",
            "time",
            "uchar",
            "wchar",
            "wctype"
        ).map { Arguments.of(it) }
    }

        /* Function signatures of some functions that we verify from the headers */
        /*
        @JvmStatic
        fun functions(): Stream<Arguments> = Stream.of(
            /* assert.h --------------------------------------------------------- */
            Arguments.of(
                "assert", "__assert_fail",
                Type.function(
                    Type.Void(),
                    Param("__assertion", Type.Ptr(Type.char.const())),
                    Param("__file", Type.Ptr(Type.char.const())),
                    Param("__line", Type.uint),
                    Param("__function", Type.Ptr(Type.char.const())),
                )
            ),

            /* stdio.h --------------------------------------------------------- */
            Arguments.of(
                "stdio", "printf",
                Type.function(
                    Type.int,
                    Param("__format", Type.Ptr(Type.char.const()).restrict()),
                    variadic = true
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
    }*/

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

        bumped(input, listOf()) { ast1, unit1 ->
            /*
            val pp1 = unit1.printer.print(ast1).assertOK().write()
            bumped(pp1, listOf()) { ast2, unit2 ->
                ast1.declarations.zip(ast2.declarations) { l, r ->
                    try {
                        roundtrip.eq(l, r.withMeta(l.meta))
                    } catch (e : Throwable) {
                        println("Pretty 1:\n" + Pretty.lhs(l))
                        println("Pretty 2:\n" + Pretty.lhs(r))
                        throw e
                    }
                }
            }
             */
        }

    }

    /*
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
    */
}