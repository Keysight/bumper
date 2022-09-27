package com.riscure.dobby.clang

import arrow.core.*
import com.riscure.dobby.clang.parser.Parser
import kotlin.test.*

internal class ParserTest {
    @Test
    fun optimizationToggle01() {
        val O0 = Parser.parseClangArguments(listOf("-O0"))
        assertTrue(O0.isRight())

        val args = (O0 as Either.Right).value.optArgs
        assertEquals(1, args.size)
        assertEquals("O0", args[0].opt.name)
    }

    @Test
    fun optimizationToggle02() {
        val line = Parser.parseClangArguments(listOf("-O", "1"))
        assertTrue(line.isRight())

        val cmd = (line as Either.Right).value
        assertEquals(listOf("O"), cmd.optArgs.map { it.opt.name })
        cmd.optArgs.map { it.values }.forEach { assertTrue(it[0].isEmpty()) }
        assertEquals(listOf("1"), cmd.positionalArgs)
    }

    @Test
    fun optimizationToggle03() {
        val O1 = Parser.parseClangArguments(listOf("-O=1"))
        assertTrue(O1.isRight())

        val args = (O1 as Either.Right).value.optArgs
        assertEquals(1, args.size)
        assertEquals("O", args[0].opt.name)

        // this is not a valid value for the option,
        // but the point is that we don't interpret the =,
        // because neither does clang for this option.
        assertEquals(listOf("=1"), args[0].values)
    }

    @Test
    fun xargJoinedAndSep() {
        val xarch = Parser.parseClangArguments(listOf("-Xarch_x", "y"))
        assertTrue(xarch.isRight())

        val args = (xarch as Either.Right).value.optArgs
        assertEquals(1, args.size)
        assertEquals("Xarch_", args[0].opt.name)
        assertEquals(listOf("x", "y"), args[0].values)
    }

    @Test
    fun mcpu() {
        val mcpu = Parser.parseClangArguments(listOf("-mcpu=hexagonv5"))
        assertTrue(mcpu.isRight())

        val args = (mcpu as Either.Right).value.optArgs
        assertEquals(1, args.size)
        assertEquals("mcpu=", args[0].opt.name)
        assertEquals(listOf("hexagonv5"), args[0].values)
    }

    @Test
    fun mcpuSeparate() {
        val ugh = Parser.parseClangArguments(listOf("-mcpu", "hexagonv5"))
        assertTrue(ugh.isLeft())
    }
}