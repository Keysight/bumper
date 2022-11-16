package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface EnumParseTest<E,S,U: UnitState<E, S>> : ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Named enum")
    fun test00() = parsed("""
        enum E { X, Y, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val enum = assertIs<Declaration.Enum>(ast.toplevelDeclarations[0])

        assertFalse(enum.isAnonymous)
        assertEquals("E".some(), enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(Visibility.TUnit, enum.visibility)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), enum.site)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L), enums[0])
        assertEquals(Enumerator("Y", 1L), enums[1])
        assertEquals(Enumerator("Z", 2L), enums[2])
    }

    @Test
    @DisplayName("Named enum with fixed enumerator value")
    fun test01() = parsed("""
        enum E { X, Y = 99, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val enum = assertIs<Declaration.Enum>(ast.toplevelDeclarations[0])

        assertFalse(enum.isAnonymous)
        assertEquals("E".some(), enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(Visibility.TUnit, enum.visibility)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), enum.site)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L), enums[0])
        assertEquals(Enumerator("Y", 99L), enums[1])
        assertEquals(Enumerator("Z", 100L), enums[2])
    }


    @Test
    @DisplayName("Named enum with expression enumerator value")
    fun test02() = parsed("""
        enum E { X, Y = 20 + 22, Z };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val enum = assertIs<Declaration.Enum>(ast.toplevelDeclarations[0])

        assertFalse(enum.isAnonymous)
        assertEquals("E".some(), enum.ident)
        assertEquals(Storage.Default, enum.storage)
        assertEquals(Visibility.TUnit, enum.visibility)
        assertEquals(EntityKind.Enum, enum.kind)
        assertTrue(enum.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), enum.site)

        val enums = enum.enumerators.assertOK()
        assertEquals(3, enums.size)
        assertEquals(Enumerator("X", 0L), enums[0])
        assertEquals(Enumerator("Y", 42L), enums[1])
        assertEquals(Enumerator("Z", 43L), enums[2])
    }

}