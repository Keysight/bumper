package com.riscure.dobby.clang

import kotlin.test.*

internal class SpecTest {

    val s = Spec.clang11

    val o = s.get("o")
    val output = s.get("_output_EQ")
    val output2 = s.get("_output")

    @Test
    fun aliasingTest() {
        assertTrue(s.equal(o, output))
        assertTrue(s.equal(o, output2))
        assertTrue(s.equal(output, o))
        assertTrue(s.equal(output, output2))
        assertTrue(s.equal(output2, output))
        assertTrue(s.equal(output2, o))
    }


    @Test
    fun outputFilter() {
        val c = Command(
            listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mv5")),
            ), listOf()
        )

        val d = c.filter(s, setOf(output2))
        assertEquals(1, d.optArgs.size)

        val e = c.filter(s, setOf(s.get("mv5")))
        assertEquals(2, e.optArgs.size)
    }

    @Test
    fun mv5Filter() {
        val c = Command(
            listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mv5")),
            ), listOf()
        )

        assertEquals(2, c.filter(s, setOf(s.get("mcpu_EQ"))).optArgs.size)

        val d = Command(
            listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mcpu_EQ"), "arm"),
            ), listOf()
        )

        assertEquals(2, c.filter(s, setOf(s.get("mv5"))).optArgs.size)
    }


    @Test
    fun filterSame() {
        val c = Command(listOf(Arg(s["o"])), listOf())
        assertEquals(0, c.filter(s, s["o"]).optArgs.size)
        assertEquals(1, c.replace(s, Arg(s["o"], listOf("someOutput.o"))).optArgs.size)
    }

    @Test
    fun filterO0() {
        val c = Command(listOf(Arg(s["O0"])), listOf())

        // This is not registered as an alias
        println(c.filter(s, s["O"]))
    }
}