package com.riscure.bumper

import com.riscure.bumper.ast.*
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.*
import kotlin.test.*

interface TypedefParseTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Typedef anonymous struct")
    fun test00() = roundtrip("""
        typedef struct { int x; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])
        assertTrue(typedef.isDefinition)
        assertEquals("MyStruct", typedef.ident)
        assertEquals(EntityKind.Typedef, typedef.kind)

        val struct = ast.structs[0]
        val typref = assertIs<Type.Struct>(typedef.underlyingType)
        assertEquals(struct.tlid, typref.ref)
    }

    @Test
    @DisplayName("Scoped typedef anonymous struct")
    fun test01() = roundtrip("""
        void f() {
            typedef struct { int x; } MyStruct;
        }
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        assertIs<UnitDeclaration.Fun<*>>(ast.declarations[0])
    }

    @Test
    @DisplayName("Typedef anonymous enum")
    fun test02() = roundtrip("""
        typedef enum { Monday } MyEnum;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])
        val enum    = assertIs<UnitDeclaration.Enum>(ast.enums[0])
        val enumRef = assertIs<Type.Enum>(typedef.underlyingType)
        assertEquals(enum.tlid, enumRef.ref)
    }

    @Test
    @DisplayName("Redeclaration of identical typedef OK")
    fun test04() = roundtrip("""
        typedef int myint;
        typedef int myint;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
    }

    @Test
    @DisplayName("Redeclaration of typedef with different type fails")
    fun test05() = invalid("""
        typedef int myint;
        typedef short myint;
    """.trimIndent())

    @Test
    @DisplayName("Typedef pointer to anonymous struct")
    fun test06() = roundtrip("""
        typedef struct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])
        val ptrType = assertIs<Type.Ptr>(typedef.underlyingType)
        val strType = assertIs<Type.Struct>(ptrType.pointeeType)
    }

    @Test
    @DisplayName("Typedef pointer to named struct")
    fun test07() = roundtrip("""
        typedef struct mystruct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val struct = ast.structs[0]
        assertEquals("mystruct", struct.ident)
    }

    @Test
    @DisplayName("Typedef struct declaration with external definition")
    fun test08() = roundtrip("""
        struct A { int member; };
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])
        val typeref = assertIs<Type.Struct>(typedef.underlyingType)
        val struct  = ast.structs[0]
        assertEquals(struct.tlid, typeref.ref)
    }

    @Test
    @DisplayName("Typedef struct declaration without definition")
    fun test10() = roundtrip("""
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        // Clang gives incomplete type A warning.
        assertEquals(2, ast.declarations.size)
    }

    @Test
    @DisplayName("Typedef struct definition of forward declaration")
    fun test11() = roundtrip("""
        struct A;
        typedef struct A { int member; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(3, ast.declarations.size)
        val structDecl = ast.structs[0]
        val structDef  = ast.structs[1]
        val typedef   = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])

        assertEquals(structDecl.mkSymbol(ast.tuid), structDef.mkSymbol(ast.tuid))
        val structRef  = assertIs<Type.Struct>(typedef.underlyingType)
        assertEquals(structDecl.tlid, structRef.ref)
    }

    @Test
    @DisplayName("Typedef forward declaration")
    fun test12() = roundtrip("""
        typedef struct A MyStruct;
        struct A { int member; };
    """.trimIndent()) { ast ->
        assertEquals(3, ast.declarations.size)
        val typedef    = assertIs<UnitDeclaration.Typedef>(ast.typedefs[0])
        val structDecl = ast.structs[0]
        val structDef  = ast.structs[1]
        val typeref    = assertIs<Type.Struct>(typedef.underlyingType)

        assertEquals(structDecl.mkSymbol(ast.tuid), structDef.mkSymbol(ast.tuid))
        assertEquals(structDecl.tlid, typeref.ref)
    }

    @Test
    @DisplayName("Typedef __builtin_va_list")
    fun test09() = roundtrip("""
        typedef __builtin_va_list va_list;
    """.trimIndent()
    ) { ast ->
        assertEquals(1, ast.declarations.size)
        val typedef  = assertIs<UnitDeclaration.Typedef>(ast.declarations[0])
        assertIs<Type.VaList>(typedef.underlyingType)
    }

    @Test
    @DisplayName("typeof")
    fun test13() = roundtrip("""
        struct A {
          int m;
        };
        typedef typeof (((struct A *)0)->m) mytype;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef  = assertIs<UnitDeclaration.Typedef>(ast.typedefs.find { it.ident == "mytype"})
        assertEquals(Type.int, typedef.underlyingType)
    }

    @Test
    @DisplayName("typedef inline enum")
    fun test14() = roundtrip("""
        typedef enum {
          TEE_MODE_ENCRYPT = 0,
          TEE_MODE_DECRYPT = 1,
          TEE_MODE_SIGN = 2,
          TEE_MODE_VERIFY = 3,
          TEE_MODE_MAC = 4,
          TEE_MODE_DIGEST = 5,
          TEE_MODE_DERIVE = 6
        } TEE_OperationMode;
    """.trimIndent()) { ast ->
        val enum = ast.enums[0].assertOK()
        val typOpMode = ast.typedefs[0].assertOK()
        assertEquals(7, enum.enumerators.assertOK().size)
    }
}