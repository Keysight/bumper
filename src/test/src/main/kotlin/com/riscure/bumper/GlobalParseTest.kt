package com.riscure.bumper

import arrow.core.some
import com.riscure.bumper.ast.*
import com.riscure.bumper.parser.UnitState
import com.riscure.dobby.clang.ClangParser
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface GlobalParseTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Global array initialization")
    fun test00() = roundtrip("""
        static const int xs[1] = { 42, };
    """.trimIndent()) { ast, unit ->
        val xs = ast.variables[0]
        val arType = assertIs<Type.Array>(xs.type)
        // assertEquals("{ 42, }", unit.  .expWriter(xs.rhs.assertOK()).assertOK())
        assertEquals(1L.some(), arType.size)
    }

    @Test
    @DisplayName("Global 2D-array initialization")
    fun test01() = roundtrip("""
        static const int xs[1][2] = { {42, 18}, };
    """.trimIndent(), ClangParser.parseOptions(listOf("-Werror=excess-initializers")).assertOK()) { ast, unit ->
        val xs = ast.variables[0]
        val arType = assertIs<Type.Array>(xs.type)

        assertEquals(1L.some(), arType.size)
    }
}
