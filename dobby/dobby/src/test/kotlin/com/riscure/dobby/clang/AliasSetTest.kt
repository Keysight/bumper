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
        val output2 = AliasSet(Spec.clang11["_output"])

        o.alias(output)
        output2.alias(o)

        assertEquals(output.representative(), output2.representative())
    }

}