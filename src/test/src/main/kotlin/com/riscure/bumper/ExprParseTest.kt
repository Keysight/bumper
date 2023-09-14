package com.riscure.bumper

import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

interface ExprParseTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U>  {

    @Test
    @DisplayName("0")
    fun test00() = roundtrip("""
    int x = 0;
    """.trimIndent())

    @Test
    @DisplayName("-1")
    fun test01() = roundtrip("""
    int x = -1;
    """.trimIndent())

    @Test
    @DisplayName("0xFF")
    fun test02() = roundtrip("""
    int x = 0xFF;
    """.trimIndent())

    @Test
    @DisplayName("0b11")
    fun test03() = roundtrip("""
    int x = 0b11;
    """.trimIndent())

    @Test
    @DisplayName("0L")
    fun test04() = roundtrip("""
    long x = 0L;
    """.trimIndent())

    @Test
    @DisplayName("1 + 1")
    fun test05() = roundtrip("""
    int x = 1 + 1;
    """.trimIndent()) { unit ->
        val x = unit.variables[0]
        val rhs = x.rhs.assertOK()
    }

}