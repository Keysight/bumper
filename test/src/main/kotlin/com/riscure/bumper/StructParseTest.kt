package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface StructParseTest<E,S,U: UnitState<E, S>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Empty anonymous struct definition")
    fun test00() = parsed("""
        struct {};
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

        assertEquals(Storage.Default, struct.storage)
        assertEquals(EntityKind.Struct, struct.kind)
        assertTrue(struct.isDefinition)
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
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

        assertEquals("A", struct.ident)
        assertEquals(Storage.Default, struct.storage)
        assertEquals(EntityKind.Struct, struct.kind)
        assertFalse(struct.isDefinition)
        assertEquals(None, struct.fields)
    }

    @Test
    @DisplayName("Empty named struct definition")
    fun test03() = parsed("""
        struct A {};
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

        assertEquals("A", struct.ident)
        assertEquals(Storage.Default, struct.storage)
        assertEquals(EntityKind.Struct, struct.kind)
        assertTrue(struct.isDefinition)
        assertEquals(listOf<Field>().some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with single member")
    fun test06() = parsed("""
        struct A { int i; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

        assertEquals(listOf(Field("i", Type.int)).some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with two members")
    fun test07() = parsed("""
        struct A { int i; double j; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

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
    fun test20() = parsed("""
        struct A { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B" })
        assertTrue(structB.isDefinition)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested struct in anonymous struct")
    fun test21() = parsed("""
        struct { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B" })
        assertTrue(structB.isDefinition)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested anonymous struct in named struct")
    fun test22() = parsed("""
        struct A { struct {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structA = assertNotNull(ast.structs.find { it.ident == "A" })
        assertTrue(structA.isDefinition)
    }

    // Anonymous members
    // =================

    @Test
    @DisplayName("Anonymous member of anonymous union type in struct")
    fun test30() = parsed("""
        struct A { union { char alpha; int num; }; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val structA = assertNotNull(ast.structs[0])

        val fields = structA.fields.assertOK()
        assertEquals(1, fields.size)

        val field = assertNotNull(fields[0])
        val unionType = assertIs<Type.Union>(field.type)

        val union = assertIs<Declaration.Composite>(ast.unions[0])
        assertEquals(StructOrUnion.Union, union.structOrUnion)
        assertEquals(union.mkSymbol(ast.tuid), unionType.ref)

        val fs = union.fields.getOrElse { fail("Expected fields") }
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
        assertEquals(2, ast.declarations.size)
        val scope = assertNotNull(ast.structs.find { it.ident == "Scope" })

        val fields = scope.fields.assertOK()
        assertEquals(1, fields.size)

        val field = assertNotNull(fields[0])
        val typeref = assertIs<Type.Struct>(field.type)
        val struct  = assertIs<Declaration.Composite>(ast.structs.find { it.ident.startsWith("__") })
        assertEquals(StructOrUnion.Struct, struct.structOrUnion)

        assertEquals(struct.mkSymbol(ast.tuid), typeref.ref)

        val fs = struct.fields.getOrElse { fail("Expected fields") }
        assertEquals(1, fs.size)
        val i = assertNotNull(fs[0])
        assertEquals(Type.int, i.type)
        assertEquals("i", i.name)
    }
}