package com.riscure.dobby.shell

import arrow.core.*
import kotlin.test.*

internal class ShellTest {

    @Test
    fun parseShellWsEscape() {
        val result = Shell.line("x\\ \\ y")
        assertEquals(listOf("x  y").right(), result.map { it.eval() })
    }


    @Test
    fun parseShellWsEscapeInQuote() {
        val result = Shell.line("\"x\\ y\"")
        assertEquals(listOf("x\\ y").right(), result.map { it.eval() })
    }

    @Test
    fun parseShellWsEscapedEscape() {
        val result = Shell.line("x\\\\ y")
        assertEquals(listOf("x\\", "y").right(), result.map { it.eval() })
    }

    @Test
    fun parsePlain() {
        val result = Shell.line("x y z")
        assertEquals(listOf("x", "y", "z").right(), result.map { it.eval() })
    }

    @Test
    fun parsePlainWithExtraSpace() {
        val result = Shell.line("x \t y  z")
        assertEquals(listOf("x", "y", "z").right(), result.map { it.eval() })
    }

    @Test
    fun parsePlainWithAdjacentQuoted() {
        val result = Shell.line("x'y'\"z\"")
        assertEquals(listOf("xyz").right(), result.map { it.eval() })
    }

    @Test
    fun parseSingleQuotedBackslash() {
        val result = Shell.line("'\\'")
        assertEquals(listOf("\\").right(), result.map { it.eval() })
    }

    @Test
    fun parseSingleQuotedDoubleBackslash() {
        val result = Shell.line("'\\\\'")
        assertEquals(listOf("\\\\").right(), result.map { it.eval() })
    }

    @Test
    fun cannotEscapeSinglequote() {
        val result = Shell.line("'\\''")
        assertTrue(result.isLeft())
    }
}