package com.riscure.dobby.clang

import kotlin.test.*

internal class AliasSetTest {
    @Test
    fun alias() {
        val o = AliasSet(Spec.clang11["o"])
        val output = AliasSet(Spec.clang11["_output_EQ"])

        o.alias(output)

        assertEquals(output, o.representative())
    }

    @Test
    fun reflexive() {
        val o = AliasSet(Spec.clang11["o"])

        assertEquals(o, o.representative())
    }

    @Test
    fun transitive() {
        val o = AliasSet(Spec.clang11["o"])
        val output = AliasSet(Spec.clang11["_output_EQ"])
        val slash_o = AliasSet(Spec.clang11["_SLASH_o"])

        o.alias(output)
        slash_o.alias(o)

        assertEquals(output, slash_o.representative())
    }

}