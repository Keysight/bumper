package com.riscure.dobby.shell

import kotlin.test.*

internal class ArgTest {

    @Test
    fun quoteValWithQuotes() {
        val s = "\"xy\""
        val v = Arg.quote(s)

        assertEquals(
            Arg(listOf(Val.DoubleQuoted(listOf(
                Symbol.escapedDouble,
                Symbol("x"),
                Symbol("y"),
                Symbol.escapedDouble
            )))),
           v
        )

        assertEquals(s, v.eval())
    }

    @Test
    fun quoteValWithWs() {
        val s = "x y"
        val v = Arg.quote(s)

        assertEquals(
            Arg(listOf(Val.DoubleQuoted(listOf(
                Symbol("x"),
                Symbol(" "),
                Symbol("y"),
            )))),
            v
        )

        assertEquals(s, v.eval())
    }

    @Test
    fun quoteValWithTab() {
        val s = "x\ty"
        val v = Arg.quote(s)

        assertEquals(
            Arg(listOf(Val.DoubleQuoted(listOf(
                Symbol("x"),
                Symbol("\t"),
                Symbol("y"),
            )))),
            v
        )

        assertEquals(s, v.eval())
    }

    @Test
    fun quoteValWithBackslash() {
        val s = "x\\y"
        val v = Arg.quote(s)

        assertEquals(
            Arg(listOf(Val.DoubleQuoted(listOf(
                Symbol("x"),
                Symbol.escapedEscape,
                Symbol("y"),
            )))),
            v
        )

        assertEquals(s, v.eval())
    }

    @Test
    fun quoteValWithoutSpecial() {
        val s = "xy"
        val v = Arg.quote(s)

        assertEquals(
            Arg(listOf(Val.Unquoted(listOf(
                Symbol("x"),
                Symbol("y"),
            )))),
            v
        )

        assertEquals(s, v.eval())
    }
}