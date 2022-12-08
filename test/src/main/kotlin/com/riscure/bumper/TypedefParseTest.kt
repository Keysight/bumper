package com.riscure.bumper

import com.riscure.bumper.ast.*
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.api.*
import kotlin.test.*

interface TypedefParseTest<E,S,U: UnitState<E, S>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Typedef anonymous struct")
    fun test00() = parsedAndRoundtrip("""
        typedef struct { int x; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.typedefs[0])
        assertTrue(typedef.isDefinition)
        assertEquals("MyStruct", typedef.ident)
        assertEquals(EntityKind.Typedef, typedef.kind)

        val struct = assertIs<Declaration.Composite>(ast.structs[0])
        val typref = assertIs<Type.Struct>(typedef.underlyingType)
        assertEquals(struct.mkSymbol(ast.tuid), typref.ref)
    }

    @Test
    @DisplayName("Scoped typedef anonymous struct")
    fun test01() = parsedAndRoundtrip("""
        void f() {
            typedef struct { int x; } MyStruct;
        }
    """.trimIndent()) { ast ->
        assertEquals(1, ast.declarations.size)
        assertIs<Declaration.Fun<*>>(ast.declarations[0])
    }

    @Test
    @DisplayName("Typedef anonymous enum")
    fun test02() = parsedAndRoundtrip("""
        typedef enum { Monday } MyEnum;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.typedefs[0])
        val enum    = assertIs<Declaration.Enum>(ast.enums[0])
        val enumRef = assertIs<Type.Enum>(typedef.underlyingType)
        assertEquals(enum.mkSymbol(ast.tuid), enumRef.ref)
    }

    @Test
    @DisplayName("Redeclaration of identical typedef OK")
    fun test04() = parsedAndRoundtrip("""
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
    fun test06() = parsedAndRoundtrip("""
        typedef struct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.typedefs[0])
        val ptrType = assertIs<Type.Ptr>(typedef.underlyingType)
        val strType = assertIs<Type.Struct>(ptrType.pointeeType)
    }

    @Test
    @DisplayName("Typedef pointer to named struct")
    fun test07() = parsedAndRoundtrip("""
        typedef struct mystruct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val struct = assertIs<Declaration.Composite>(ast.structs[0])
        assertEquals("mystruct", struct.ident)
    }

    @Test
    @DisplayName("Typedef struct declaration with external definition")
    fun test08() = parsedAndRoundtrip("""
        struct A { int member; };
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.declarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.typedefs[0])
        val typeref = assertIs<Type.Struct>(typedef.underlyingType)
        val struct  = assertIs<Declaration.Composite>(ast.structs[0])
        assertEquals(struct.mkSymbol(ast.tuid), typeref.ref)
    }

    @Test
    @DisplayName("Typedef struct declaration without definition")
    fun test10() = parsedAndRoundtrip("""
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        // Clang gives incomplete type A warning.
        assertEquals(2, ast.declarations.size)
    }

    @Test
    @DisplayName("Typedef struct definition of forward declaration")
    fun test11() = parsedAndRoundtrip("""
        struct A;
        typedef struct A { int member; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(3, ast.declarations.size)
        val structDecl = assertIs<Declaration.Composite>(ast.structs[0])
        val structDef  = assertIs<Declaration.Composite>(ast.structs[1])
        val typedef    = assertIs<Declaration.Typedef>(ast.typedefs[0])

        assertEquals(structDecl.mkSymbol(ast.tuid), structDef.mkSymbol(ast.tuid))
        val structRef  = assertIs<Type.Struct>(typedef.underlyingType)
        assertEquals(structDecl.mkSymbol(ast.tuid), structRef.ref)
    }

    @Test
    @DisplayName("Typedef forward declaration")
    fun test12() = parsedAndRoundtrip("""
        typedef struct A MyStruct;
        struct A { int member; };
    """.trimIndent()) { ast ->
        assertEquals(3, ast.declarations.size)
        val typedef    = assertIs<Declaration.Typedef>(ast.typedefs[0])
        val structDecl = assertIs<Declaration.Composite>(ast.structs[0])
        val structDef  = assertIs<Declaration.Composite>(ast.structs[1])
        val typeref    = assertIs<Type.Struct>(typedef.underlyingType)

        assertEquals(structDecl.mkSymbol(ast.tuid), structDef.mkSymbol(ast.tuid))
        assertEquals(structDecl.mkSymbol(ast.tuid), typeref.ref)
    }

    @Test
    @DisplayName("Typedef __builtin_va_list")
    fun test09() = parsedAndRoundtrip("""
        typedef __builtin_va_list va_list;
    """.trimIndent()
    ) { ast ->
        assertEquals(1, ast.declarations.size)
        val typedef  = assertIs<Declaration.Typedef>(ast.declarations[0])
        assertIs<Type.VaList>(typedef.underlyingType)
    }
}