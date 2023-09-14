package com.riscure.bumper

import arrow.core.getOrElse
import arrow.core.some
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Storage
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
        assertEquals(listOf(TypeQualifier.Constant), f.type.returnType.attrsOnType)
    }

    @Test
    @DisplayName("Aligned empty anonymous struct definition")
    fun test02() = roundtrip("""
        struct __attribute__((aligned(4))) {};
    """.trimIndent()) { ast ->
            assertEquals(1, ast.declarations.size)
            val struct = ast.structs[0]

            assertEquals(Storage.Default, struct.storage)
            assertEquals(1, struct.attributes.size)
            assertTrue(struct.attributes.get(0) is Attr.AlignAs )
            assertEquals(4, (struct.attributes.get(0) as Attr.AlignAs).alignment)
            assertEquals(EntityKind.Struct, struct.kind)
            assertTrue(struct.isDefinition)
            assertEquals(listOf<Field>().some(), struct.fields)
        }

    @Test
    @DisplayName("Volatile named struct declaration")
    fun test03() =
        roundtrip("""
        struct a {};
        volatile struct a A;
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)
            val varA = ast.variables[0]
            assertTrue(varA.type is Type.Struct)
            assertEquals(1, (varA.type as Type.Struct).attrsOnType.size)
            assertTrue((varA.type as Type.Struct).attrsOnType.get(0) is TypeQualifier.Volatile )
        }

    @Test
    @DisplayName("Aligned empty named struct definition")
    fun test04() = roundtrip("""
        struct __attribute__((aligned(16))) A {};
    """.trimIndent()) { ast ->
            assertEquals(1, ast.declarations.size)
            val struct = ast.structs[0]

            assertEquals("A", struct.ident)
            assertEquals(1, struct.attributes.size)
            assertTrue(struct.attributes.get(0) is Attr.AlignAs )
            assertEquals(16, (struct.attributes.get(0) as Attr.AlignAs).alignment)
            assertEquals(Storage.Default, struct.storage)
            assertEquals(EntityKind.Struct, struct.kind)
            assertTrue(struct.isDefinition)
            assertEquals(listOf<Field>().some(), struct.fields)
        }

    @DisplayName("Named struct const with single member")
    fun test05() =
        roundtrip("""
        struct a { int i; };
        struct const a A;
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)
            val varA = ast.variables[0]
            assertTrue(varA.type is Type.Struct)
            assertEquals(1, (varA.type as Type.Struct).attrsOnType.size)
            assertTrue((varA.type as Type.Struct).attrsOnType.get(0) is TypeQualifier.Constant)

            val struct = ast.structs[0]
            assertEquals(listOf(Field("i", Type.int)).some(), struct.fields)
        }

    @Test
    @DisplayName("Const named struct with single member")
    fun test06() =
        roundtrip("""
        struct a { int i; };
        const struct a A;
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)
            val varA = ast.variables[0]
            assertTrue(varA.type is Type.Struct)
            assertEquals(1, (varA.type as Type.Struct).attrsOnType.size)
            assertTrue((varA.type as Type.Struct).attrsOnType.get(0) is TypeQualifier.Constant)

            val struct = ast.structs[0]
            assertEquals(listOf(Field("i", Type.int)).some(), struct.fields)
        }

    @Test
    @DisplayName("Named struct packed with two members")
    fun test07() = roundtrip("""
        struct __attribute__((packed)) A { int i; double j; };
    """.trimIndent()) { ast ->
            assertEquals(1, ast.declarations.size)
            val struct = ast.structs[0]
            assertEquals(1, struct.attributes.size)
            assertTrue(struct.attributes.get(0) is Attr.Packed)

            assertEquals(
                listOf(
                    Field("i", Type.int),
                    Field("j", Type.double),
                ).some(), struct.fields
            )
        }

    @Test
    @DisplayName("Packed named struct with two members")
    fun test08() = roundtrip("""
        __attribute__((packed)) struct A { int i; double j; };
    """.trimIndent()) { ast ->
            assertEquals(1, ast.declarations.size)
            val struct = ast.structs[0]
            assertEquals(0, struct.attributes.size)

            assertEquals(
                listOf(
                    Field("i", Type.int),
                    Field("j", Type.double),
                ).some(), struct.fields
            )
        }

    @Test
    @DisplayName("Packed nested named struct in named struct")
    fun test09() =
        roundtrip("""
        struct __attribute__((packed)) B { int i; };
        struct A { struct B  b; };
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)

            val structB = assertNotNull(ast.structs.find { it.ident == "B" })
            assertTrue(structB.isDefinition)
            assertEquals(1, structB.attributes.size)
            assertTrue(structB.attributes.get(0) is Attr.Packed)
            assertEquals(listOf(Field("i", Type.int)).some(), structB.fields)
        }

    @Test
    @DisplayName("Packed nested named struct in named struct")
    fun test10() =
        roundtrip("""
        struct A { struct __attribute__((packed)) B { int i; } b; };
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)

            val structB = assertNotNull(ast.structs.find { it.ident == "B" })
            assertTrue(structB.isDefinition)
            assertEquals(1, structB.attributes.size)
            assertTrue(structB.attributes.get(0) is Attr.Packed)
            assertEquals(listOf(Field("i", Type.int)).some(), structB.fields)
        }

    @Test
    @DisplayName("Nested named struct const in named struct")
    fun test11() =
        roundtrip("""
        struct A { const struct B { int i; } b; };
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)

            val structB = assertNotNull(ast.structs.find { it.ident == "B" })
            assertTrue(structB.isDefinition)
            assertEquals(0, structB.attributes.size)
            assertEquals(listOf(Field("i", Type.int)).some(), structB.fields)
            val structA = assertNotNull(ast.structs.find { it.ident == "A" })
            assertTrue(structA.isDefinition)
            assertEquals(0, structA.attributes.size)
            var fields = structA.fields.getOrElse { listOf() }
            assertEquals(1, fields.size)
            assertTrue((fields[0] as Field.Leaf).type is Type.Struct)
            assertEquals(1, (fields[0] as Field.Leaf).type.attrsOnType.size)
            assertTrue((fields[0] as Field.Leaf).type.attrsOnType[0] is TypeQualifier.Constant)
        }

    @Test
    @DisplayName("Unsupported attribute in named nested struct")
    fun test12() = roundtrip("""
        struct __attribute__((bla)) A { struct B {} b; };
    """.trimIndent()) { ast ->
            assertEquals(2, ast.declarations.size)
            val structA = assertNotNull(ast.structs.find { it.ident == "A" })
            assertEquals(0, structA.attributes.size)

            val structB = assertNotNull(ast.structs.find { it.ident == "B" })
            assertTrue(structB.isDefinition)
            assertEquals(listOf<Field>().some(), structB.fields)
        }

    @Test
    @DisplayName("Pointer to constant int 1")
    fun test13() = roundtrip("""
        int const *ptr;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(0, ptr.type.attrsOnType.size)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(1, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
        assertTrue((ptr.type as Type.Ptr).pointeeType.attrsOnType[0] is TypeQualifier.Constant)
    }

    @Test
    @DisplayName("Pointer to constant int 2")
    fun test14() = roundtrip("""
        const int *ptr;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(0, ptr.type.attrsOnType.size)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(1, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
        assertTrue((ptr.type as Type.Ptr).pointeeType.attrsOnType[0] is TypeQualifier.Constant)
    }

    @Test
    @DisplayName("Constant pointer to int 1")
    fun test15() = roundtrip("""
        int *const ptr;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(1, ptr.type.attrsOnType.size)
        assertTrue(ptr.type.attrsOnType[0] is TypeQualifier.Constant)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(0, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
    }

    @Test
    @DisplayName("Constant pointer to int 2")
    fun test16() = roundtrip("""
        int (*const ptr);
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(1, ptr.type.attrsOnType.size)
        assertTrue(ptr.type.attrsOnType[0] is TypeQualifier.Constant)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(0, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
    }

    @Test
    @DisplayName("Constant pointer to const int 1")
    fun test17() = roundtrip("""
        const int *const ptr;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(1, ptr.type.attrsOnType.size)
        assertTrue(ptr.type.attrsOnType[0] is TypeQualifier.Constant)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(1, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
        assertTrue((ptr.type as Type.Ptr).pointeeType.attrsOnType[0] is TypeQualifier.Constant)
    }

    @Test
    @DisplayName("Constant pointer to const int 2")
    fun test18() = roundtrip("""
        int const *const ptr;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        val ptr = assertNotNull(ast.variables.find { it.ident == "ptr" })
        assertEquals(1, ptr.type.attrsOnType.size)
        assertTrue(ptr.type.attrsOnType[0] is TypeQualifier.Constant)
        assertTrue(ptr.type is Type.Ptr)
        assertEquals(1, (ptr.type as Type.Ptr).pointeeType.attrsOnType.size)
        assertTrue((ptr.type as Type.Ptr).pointeeType.attrsOnType[0] is TypeQualifier.Constant)
    }
}