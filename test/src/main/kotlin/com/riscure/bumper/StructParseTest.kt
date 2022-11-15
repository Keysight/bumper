package com.riscure.bumper

import arrow.core.*
import com.riscure.langs.c.Frontend
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface StructParseTest<E,S,U:UnitState<E,S>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Empty anonymous struct definition")
    fun test00() = parsed("""
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
    fun test02() = parsed("""
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
    fun test03() = parsed("""
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
    fun test06() = parsed("""
        struct A { int i; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        val site = Site.root + Site.Toplevel(0)
        assertEquals(listOf(
            Field(site + Site.Member(0), "i".some(), Type.int)
        ).some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with two members")
    fun test07() = parsed("""
        struct A { int i; double j; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val struct = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])

        val site = Site.root + Site.Toplevel(0)
        assertEquals(listOf(
            Field(site + Site.Member(0), "i".some(), Type.int),
            Field(site + Site.Member(1), "j".some(), Type.double),
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
    fun test20() = parsed("""
        struct A { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B".some() })
        assertEquals(Site.root + Site.Toplevel(0) + Site.Member(0), structB.site)
        assertTrue(structB.isDefinition)
        assertFalse(structB.isAnonymous)
        assertEquals(Visibility.TUnit, structB.visibility)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested struct in anonymous struct")
    fun test21() = parsed("""
        struct { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B".some() })
        assertEquals(Site.root + Site.Toplevel(0) + Site.Member(0), structB.site)
        assertTrue(structB.isDefinition)
        assertFalse(structB.isAnonymous)
        assertEquals(Visibility.TUnit, structB.visibility)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested anonymous struct in named struct")
    fun test22() = parsed("""
        struct A { struct {} b; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)

        val structA = assertNotNull(ast.structs.find { it.ident == "A".some() })
            assertEquals(Site.root + Site.Toplevel(0), structA.site)
        assertTrue(structA.isDefinition)
        assertFalse(structA.isAnonymous)
        assertEquals(Visibility.TUnit, structA.visibility)
    }

    // Anonymous members
    // =================

    @Test
    @DisplayName("Anonymous member in struct")
    fun test30() = parsed("""
        struct A { union { char alpha; int num; }; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val structA = assertNotNull(ast.structs.find { it.ident == "A".some() })

        val fields = structA.fields.assertOK()
        assertEquals(1, fields.size)

        val field = assertNotNull(fields[0])
        assertTrue(field.isAnonymous)
        assertEquals(None, field.name)

        val typeDecl = assertIs<Type.InlineDeclaration>(field.type)
        val type = assertIs<Declaration.Composite>(typeDecl.declaration)
        assertTrue(type.isAnonymous)
        assertEquals(StructOrUnion.Union, type.structOrUnion)

        val fs = type.fields.getOrElse { fail("Expected fields") }
        assertEquals(2, fs.size)
        val alpha = assertNotNull(fs[0])
        val num   = assertNotNull(fs[1])
        assertEquals(Type.char, alpha.type)
        assertEquals(Type.int, num.type)
    }

    @Test
    @DisplayName("Nested anonymous struct in anonymous struct member")
    fun test31() = parsed("""
        struct Scope { struct { int i; }; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        assertEquals(2, ast.declarations.size)
        val scope = assertNotNull(ast.structs.find { it.ident == "Scope".some() })

        val fields = scope.fields.assertOK()
        assertEquals(1, fields.size)

        val field = assertNotNull(fields[0])
        assertTrue(field.isAnonymous)
        assertEquals(None, field.name)

        val typeDecl = assertIs<Type.InlineDeclaration>(field.type)
        val type = assertIs<Declaration.Composite>(typeDecl.declaration)
        assertTrue(type.isAnonymous)
        assertEquals(StructOrUnion.Struct, type.structOrUnion)

        val fs = type.fields.getOrElse { fail("Expected fields") }
        assertEquals(1, fs.size)
        val i = assertNotNull(fs[0])
        assertEquals(Type.int, i.type)
        assertEquals("i".some(), i.name)
    }
}