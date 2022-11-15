package com.riscure.bumper

import arrow.core.*
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.*

interface TypedefParseTest<E,S,U: UnitState<E, S>>: ParseTestBase<E, S, U> {

    @Test
    @DisplayName("Typedef anonymous struct")
    fun test00() = parsed("""
        typedef struct { int x; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.toplevelDeclarations[0])

        assertFalse(typedef.isAnonymous)
        assertEquals("MyStruct".some(), typedef.ident)
        assertEquals(Visibility.TUnit, typedef.visibility)
        assertEquals(EntityKind.Typedef, typedef.kind)
        assertTrue(typedef.isDefinition)
        assertEquals(Site.root + Site.Toplevel(0), typedef.site)
    }

    @Test
    @DisplayName("Scoped typedef anonymous struct")
    fun test01() = parsed("""
        void f() {
            typedef struct { int x; } MyStruct;
        }
    """.trimIndent()) { ast ->
        // clang lifts some declaration nodes to the toplevel of the ast,
        // we check that that does not happen for locally declared typedefs
        assertEquals(1, ast.toplevelDeclarations.size)
        assertIs<Declaration.Fun<*>>(ast.toplevelDeclarations[0])
    }

    @Test
    @DisplayName("Typedef anonymous enum")
    fun test02() = parsed("""
        typedef enum { Monday } MyEnum;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
    }

    @Test
    @DisplayName("Anonymous typedef doesn't parse")
    fun test03() = invalid("""
        typedef int;
    """.trimIndent())

    @Test
    @DisplayName("Redeclaration of identical typedef OK")
    fun test04() = parsed("""
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
    fun test06() = parsed("""
        typedef struct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
    }

    @Test
    @DisplayName("Typedef pointer to named struct")
    fun test07() = parsed("""
        typedef struct mystruct { int member; } *mytyp;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
    }

    @Test
    @DisplayName("Typedef struct declaration without definition")
    fun test08() = parsed("""
        struct A { int member; };
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.toplevelDeclarations.size)
        val typedef = assertIs<Declaration.Typedef>(ast.toplevelDeclarations[1])
        val decl = assertIs<Type.InlineDeclaration>(typedef.underlyingType)
        val structDecl = assertIs<Declaration.Composite>(decl.declaration)
        assertEquals("A".some(), structDecl.ident)
        assertEquals(EntityKind.Struct, structDecl.kind)
        assertFalse(structDecl.isDefinition)
    }

    @Test
    @DisplayName("Typedef struct declaration without definition")
    fun test10() = parsed("""
        typedef struct A MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
    }

    @Test
    @DisplayName("Typedef struct definition of forward declaration")
    fun test11() = parsed("""
        struct A;
        typedef struct A { int member; } MyStruct;
    """.trimIndent()) { ast ->
        assertEquals(2, ast.toplevelDeclarations.size)
        assertEquals(3, ast.declarations.size)
        val structDecl = assertIs<Declaration.Composite>(ast.toplevelDeclarations[0])
        val typedef    = assertIs<Declaration.Typedef>(ast.toplevelDeclarations[1])
        val inline     = assertIs<Type.InlineDeclaration>(typedef.underlyingType)
        val structDef  = assertIs<Declaration.Composite>(inline.declaration)
        assertFalse(structDecl.isDefinition)
        assertTrue(structDef.isDefinition)
    }

    @Test
    @DisplayName("Typedef forward declaration")
    fun test12() = parsed("""
        typedef struct A MyStruct;
        struct A { int member; };
    """.trimIndent()) { ast ->
        assertEquals(2, ast.toplevelDeclarations.size)
        assertEquals(3, ast.declarations.size)
        val typedef    = assertIs<Declaration.Typedef>(ast.toplevelDeclarations[0])
        val def    = assertIs<Declaration.Composite>(ast.toplevelDeclarations[1])
        val inline = assertIs<Type.InlineDeclaration>(typedef.underlyingType)
        val decl   = assertIs<Declaration.Composite>(inline.declaration)
        assertFalse(decl.isDefinition)
        assertTrue(def.isDefinition)
    }

    @Test
    @DisplayName("Typedef __builtin_va_list")
    fun test09() = parsed("""
        typedef __builtin_va_list va_list;
    """.trimIndent()
    ) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
        val typedef  = assertIs<Declaration.Typedef>(ast.toplevelDeclarations[0])
        val builtin   = assertIs<Type.Typedeffed>(typedef.underlyingType)
        assertTrue(builtin.ref.resolution.site.isBuiltin())
    }
}