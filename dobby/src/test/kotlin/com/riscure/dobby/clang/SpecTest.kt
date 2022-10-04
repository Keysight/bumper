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
        with (s) {
            val c = Command(listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mv5")),
            ), listOf())
            println(c.filter(setOf(output2)))
        }
    }

    @Test
    fun mv5Filter() {
        with (s) {
            val c = Command(listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mv5")),
            ), listOf())

            println(c.filter(setOf(s.get("mcpu_EQ"))))

            val d = Command(listOf(
                Arg(o, "hi"),
                Arg(output, "another"),
                Arg(s.get("mcpu_EQ"), "arm"),
            ), listOf())

            println(c.filter(setOf(s.get("mv5"))))
        }
    }
}