package com.riscure.dobby.clang

import arrow.core.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.*

internal class ShellTest {

    // Argument arrays for roundtrip tests.
    // This cannot be an arbitrary set of options, because
    // we normalize to a particular representation during pretty-printing.
    companion object {
        @JvmStatic
        fun roundtrip(): List<Arguments> = listOf(
            listOf("-Xclang", "-load", "-Xclang", "/some/path/to/file.so"),
            listOf("-fsanitize=fuzzer,address"),
            listOf("-c", "some/file.c")
        ).map { Arguments.of(it) }

        fun <E, A> Either<E, A>.assertOK() =
            assertIs<Either.Right<A>>(this)
    }

    @ParameterizedTest
    @MethodSource("roundtrip")
    fun testRoundtrip(args: List<String>) {
        val cmd = Command
            .reads(args)
            .assertOK()
            .value

        val args2 = cmd
            .toArguments()

        assertEquals(args, args2)
    }
}
