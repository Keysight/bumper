package com.riscure.bumper

import com.riscure.bumper.ParseTestBase.Companion.stdopts
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.*

private val uint32 = Type.typedef("__uint32_t")
private val uint64 = Type.typedef("__uint64_t")
private val wchar_t = Type.typedef("wchar_t")

/**
 * In this test, we parse and pretty-print some standard headers.
 */
interface StdHeadersTest<E,S,U: UnitState<E, S, U>> : ParseTestBase<E, S, U> {

    companion object {
        // https://en.cppreference.com/w/c/header
        @JvmStatic
        fun headers(): Stream<Arguments> = Stream.of(
            "assert",
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
            "threads",
            "time",
            "uchar",
            "wchar",
            "wctype",
            // "complex", // complex not yet supported
            // "tgmath",  // this depends on clang's support for the overloaded pragma, which we can't parse easily.
        ).map { Arguments.of(it) }

        /* Function signatures of some functions that we verify from the headers */
        @JvmStatic
        fun functions(): Stream<Arguments> = Stream.of(
                Arguments.of(
                        "assert", "__assert_fail",
                        Type.function(
                                Type.Void(),
                                Param("__assertion", Type.Ptr(Type.char.const())),
                                Param("__file", Type.Ptr(Type.char.const())),
                                Param("__line", Type.uint),
                                Param("__function", Type.Ptr(Type.char.const())),
                        ),
                        Storage.Extern
                ),
                Arguments.of(
                        "stdio", "printf",
                        Type.function(
                                Type.int,
                                Param("__format", Type.Ptr(Type.char.const()).restrict()),
                                variadic = true
                        ),
                        Storage.Extern
                ),
                Arguments.of(
                        "setjmp", "setjmp",
                        Type.function(
                                Type.int,
                                Param("__env", Type.typedef("jmp_buf"))
                        ),
                        Storage.Extern
                ),
                Arguments.of(
                        "netdb", "gethostbyname",
                        Type.function(
                                Type.struct("hostent").ptr(),
                                Param(
                                        "__name",
                                        Type.char.const().ptr()
                                )
                        ),
                        Storage.Extern
                ),
                Arguments.of(
                        "printf", "register_printf_modifier",
                        Type.function(
                                Type.int,
                                Param("__str", wchar_t.const().ptr())
                        ),
                        Storage.Extern
                ),
                Arguments.of(
                        "printf", "register_printf_modifier",
                        Type.function(
                                Type.int,
                                Param("__str", wchar_t.const().ptr())
                        ),
                    Storage.Extern
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
                                Field("__glibc_reserved1", Type.array(uint32, 7L))
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
        roundtrip(input, stdopts)
    }

    @ParameterizedTest(name = "{0}.h:{1} function type")
    @MethodSource("functions")
    fun testStdFunction(header: String, ident: String, type: Type, storage: Storage) {
        val input = "#include <$header.h>"
        bumped(input, stdopts) { ast: TranslationUnit<E, S>, _: U ->
            val element = assertNotNull(ast.functions.find { it.ident == ident })
            eq(type, element.type)
            assertEquals(storage, element.storage)
        }
    }

    @ParameterizedTest(name = "{0}.h:{1} struct declaration ")
    @MethodSource("structs")
    fun testStdStructs(header: String, ident: String, fields: List<Field>) {
        val input = "#include <$header.h>"
        bumped(input, stdopts) { ast: TranslationUnit<E, S>, unit: U ->
            val element = ast.structs.find { it.ident == ident }
            eq(fields, assertNotNull(element).fields.assertOK())
        }
    }
}