package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class StructParseTest: ParseTestBase() {

    @Test
    @DisplayName("Empty anonymous struct definition")
    fun test00() = literal("""
        struct {};
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        assertTrue(struct.isAnonymous)
        assertEquals(None, struct.ident)
        assertEquals(Storage.Default, struct.storage)
        assertEquals(Visibility.TUnit, struct.visibility)
        assertEquals(EntityKind.Struct, struct.kind)
        assertTrue(struct.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), struct.site)
        assertEquals(listOf<Field>().some(), struct.fields)
    }

    @Test
    @DisplayName("Empty anonymous struct declaration does not parse")
    fun test01() = invalid("""
        struct;
    """.trimIndent())

    @Test
    @DisplayName("Named struct declaration")
    fun test02() = literal("""
        struct A;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        assertFalse(struct.isAnonymous)
        assertEquals("A".some(), struct.ident)
        assertEquals(Storage.Default, struct.storage)
        assertEquals(Visibility.TUnit, struct.visibility)
        assertEquals(EntityKind.Struct, struct.kind)
        assertFalse(struct.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), struct.site)
        assertEquals(None, struct.fields)
    }

    @Test
    @DisplayName("Empty named struct definition")
    fun test03() = literal("""
        struct A {};
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        assertFalse(struct.isAnonymous)
        assertEquals("A".some(), struct.ident)
        assertEquals(Storage.Default, struct.storage)
        assertEquals(Visibility.TUnit, struct.visibility)
        assertEquals(EntityKind.Struct, struct.kind)
        assertTrue(struct.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), struct.site)
        assertEquals(listOf<Field>().some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with single member")
    fun test06() = literal("""
        struct A { int i; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        assertEquals(listOf(
            Field("i", Type.int)
        ).some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with two members")
    fun test07() = literal("""
        struct A { int i; double j; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        assertEquals(listOf(
            Field("i", Type.int),
            Field("j", Type.double),
        ).some(), struct.fields)
    }

    @Test
    @DisplayName("Void member not allowed")
    fun test08() = invalid("""
        struct { void i; };
    """.trimIndent())

    // Nested Structs
    // ==============

    @Test
    @DisplayName("Nested named struct in named struct")
    fun test20() = literal("""
        struct A { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B".some() })
        assertEquals(Site.root + Site.Toplevel(0) + Site.Member("b"), structB.site)
        assertTrue(structB.isDefinition)
        assertFalse(structB.isAnonymous)
        assertEquals(Visibility.TUnit, structB.visibility)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested struct in anonymous struct")
    fun test21() = literal("""
        struct { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B".some() })
        assertEquals(Site.root + Site.Toplevel(0) + Site.Member("b"), structB.site)
        assertTrue(structB.isDefinition)
        assertFalse(structB.isAnonymous)
        assertEquals(Visibility.TUnit, structB.visibility)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested struct in anonymous struct member")
    fun test22() = literal("""
        struct { struct B {}; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B".some() })
        assertEquals(Site.root + Site.Toplevel(0) + Site.Member("b"), structB.site)
        assertTrue(structB.isDefinition)
        assertFalse(structB.isAnonymous)
        assertEquals(Visibility.TUnit, structB.visibility)
        assertEquals(listOf<Field>().some(), structB.fields)
    }
}