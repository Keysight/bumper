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
    fun test00() = parsedAndRoundtrip("""
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
    fun test02() = parsedAndRoundtrip("""
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
    fun test03() = parsedAndRoundtrip("""
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
    fun test06() = parsedAndRoundtrip("""
        struct A { int i; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.declarations[0])

        assertEquals(listOf(Field("i", Type.int)).some(), struct.fields)
    }

    @Test
    @DisplayName("Named struct with two members")
    fun test07() = parsedAndRoundtrip("""
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
    fun test20() = parsedAndRoundtrip("""
        struct A { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B" })
        assertTrue(structB.isDefinition)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested struct in anonymous struct")
    fun test21() = parsedAndRoundtrip("""
        struct { struct B {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structB = assertNotNull(ast.structs.find { it.ident == "B" })
        assertTrue(structB.isDefinition)
        assertEquals(listOf<Field>().some(), structB.fields)
    }

    @Test
    @DisplayName("Nested anonymous struct in named struct")
    fun test22() = parsedAndRoundtrip("""
        struct A { struct {} b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)

        val structA = assertNotNull(ast.structs.find { it.ident == "A" })
        assertTrue(structA.isDefinition)
    }

    // Anonymous members
    // =================

    @Test
    @DisplayName("Ignored anonymous struct field")
    fun test29() = parsedAndRoundtrip("""
        struct s {
            int;
            unsigned int x;
        };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.structs[0])
        val fields = struct.fields.assertOK()
        assertEquals(1, fields.size)
        assertEquals("x", fields[0].name)
    }

    @Test
    @DisplayName("Anonymous union member in struct")
    fun test30() = parsedAndRoundtrip("""
        struct A { union { char alpha; int num; }; };
        void f(struct A a) { a.num = 42; } // check if union members are accessible
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val structA = assertNotNull(ast.structs.find { it.ident == "A" })

        val fields = structA.fields.assertOK()
        assertEquals(1, fields.size)

        val field = assertNotNull(fields[0])
        val union = assertIs<FieldType.AnonComposite>(field.type)

        assertEquals(StructOrUnion.Union, union.structOrUnion)
        val fs = union.fields.getOrElse { fail("Expected fields") }
        assertEquals(2, fs.size)
        val alpha = assertNotNull(fs[0])
        val num   = assertNotNull(fs[1])
        assertEquals(Type.char, alpha.type)
        assertEquals(Type.int, num.type)
    }

    @Test
    @DisplayName("Nested anonymous struct in anonymous struct member")
    fun test31() = parsedAndRoundtrip("""
        struct Scope { struct { int i; }; };
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val scope = assertNotNull(ast.structs.find { it.ident == "Scope" })

        val fields = scope.fields.assertOK()
        assertEquals(1, fields.size)

        val field  = assertNotNull(fields[0])
        val struct = assertIs<FieldType.AnonComposite>(field.type)
        assertEquals(StructOrUnion.Struct, struct.structOrUnion)

        val fs = struct.fields.getOrElse { fail("Expected fields") }
        assertEquals(1, fs.size)
        val i = assertNotNull(fs[0])
        assertEquals(Type.int, i.type)
        assertEquals("i", i.name)
    }

    @Test
    @DisplayName("Field with incomplete type does not parse")
    fun test32() = invalid("""
        struct B;
        struct A { struct B b; };
        struct B { int i; };
    """.trimIndent())

    // Incomplete and complete types edge cases
    // ========================================

    @Test
    @DisplayName("Pointer to forward declaration of struct is complete")
    fun test41() = parsedAndRoundtrip("""
        struct A { struct B *b; }; // struct B is incomplete, but under pointer, so type A is complete
        struct B { int i; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val structA = assertNotNull(ast.structs.find { it.ident == "A" })
        assertTrue(structA.isDefinition)
    }

    @Test
    @DisplayName("Inline defined struct field is complete")
    fun test42() = parsedAndRoundtrip("""
        struct A { struct B { int i; } b; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val structA = assertNotNull(ast.structs.find { it.ident == "A" })
        assertTrue(structA.isDefinition)
    }

    // The point of this test case is to make sure we elaborate in a way
    // that respects type dependencies and visibility of typedefs.
    // It shouldn't move the definition of struct B in a way that its dependencies are no longer
    // visible to the definition.
    // (Ideally it should also not extend the visibility of definitions, e.g. by moving both MyInt and
    // struct B above struct A. But this property is not tested.)
    @Test
    @DisplayName("Pointer to forward declaration of struct respects dependencies")
    fun test43() = parsedAndRoundtrip("""
        struct A { struct B *b; };
        typedef int MyInt;
        struct B { MyInt i; };
    """.trimIndent()) { ast ->
        assertEquals(3, ast.declarations.size)
        val structA = assertNotNull(ast.structs.find { it.ident == "A" })
        assertTrue(structA.isDefinition)
    }

    @Test
    @DisplayName("Field that forward references a struct definition is incomplete")
    fun test44() = invalid("""
        struct A { struct B b; };
        struct B { int i; };
    """.trimIndent())

    @Test
    @DisplayName("Recursive struct is incomplete")
    fun test45() = invalid("""
        struct A { struct A a; };
    """.trimIndent())

    @Test
    @DisplayName("Recursive struct through pointer is complete")
    fun test46() = parsedAndRoundtrip("""
        struct A { struct A *a; };
    """.trimIndent()) {}

    // Extracted unit tests
    // ====================

    @Test
    @DisplayName("Tricky union fields")
    fun test47() = parsedAndRoundtrip("""
        struct __pthread_cond_s
        {
          __extension__ union
          {
            __extension__ unsigned long long int
        __wseq;
            struct
            {
              unsigned int __low;
              unsigned int __high;
            } __wseq32;
          };
          __extension__ union
          {
            __extension__ unsigned long long int
        __g1_start;
            struct
            {
              unsigned int __low;
              unsigned int __high;
            } __g1_start32;
          };
          unsigned int __g_refs[2] ;
          unsigned int __g_size[2];
          unsigned int __g1_orig_size;
          unsigned int __wrefs;
          unsigned int __g_signals[2];
        };
   """.trimIndent()) {}
}