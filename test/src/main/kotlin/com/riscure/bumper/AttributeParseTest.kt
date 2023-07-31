package com.riscure.bumper

import com.riscure.bumper.ast.Attr
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface AttributeParseTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U>  {

    @Test
    @DisplayName("weak function definition")
    fun test00() = roundtrip("""
        __attribute__((weak)) void f() {}
    """.trimIndent()) { ast, unit ->
        val f = ast.functions.find { it.ident == "f" }.assertOK()
        assertEquals(listOf(Attr.Weak), f.type.attrsOnType)
        assertTrue(f.isWeak)
    }

    @Test
    @DisplayName("const return type")
    fun test01() = roundtrip("""
        const int f() {}
    """.trimIndent()) { ast, unit ->
        val f = ast.functions.find { it.ident == "f" }.assertOK()
        assertEquals(listOf(), f.type.attrsOnType)
        assertEquals(listOf(Attr.Constant), f.type.returnType.attrsOnType)
    }

}