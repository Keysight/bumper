package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class FunctionParseTest: ParseTestBase() {

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
}