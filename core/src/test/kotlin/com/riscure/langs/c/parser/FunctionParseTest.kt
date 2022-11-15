package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.Symbol
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

interface FunctionParseTest<E,S,U:UnitState<E,S>>: ParseTestBase<E,S,U> {

    @Test
    @DisplayName("Empty main")
    fun test00() = parsed("""
        void main() {}
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)

        assertEquals(1, ast.toplevelDeclarations.size)
        val function = assertIs<Declaration.Fun<*>>(ast.toplevelDeclarations[0])

        assertFalse(function.isAnonymous)
        assertEquals("main".some(), function.ident)
        assertEquals(Storage.Default, function.storage)
        assertEquals(Visibility.TUnit, function.visibility)
        assertEquals(EntityKind.Fun, function.kind)
        assertTrue(function.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), function.site)
    }

    @Test
    @DisplayName("Function param local struct definition reference")
    fun test01() = parsed("""
        void f(struct A { int i; } a1, struct A a2);
    """.trimIndent()){ ast ->
        val f = assertNotNull(ast.functions[0])

        assertEquals(2, f.params.size)
        val a1 = assertNotNull(f.params[0])
        val a2 = assertNotNull(f.params[1])

        val t1 = assertIs<Type.InlineDeclaration>(a1.type)
        val t2 = assertIs<Type.InlineDeclaration>(a2.type)
        assertTrue(t1.declaration.isDefinition)
        assertFalse(t2.declaration.isDefinition)

        val s2 = t2.declaration.mkSymbol(ast.tuid).getOrElse { fail() }
        val def = assertNotNull(ast.definitions[s2])
        assertEquals(t1.declaration.site, def.site)
    }
}