package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.*

class TypedefParseTest: ParseTestBase() {

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
        assertEquals(1, ast.toplevelDeclarations.size)
    }


    @Test
    @DisplayName("Typedef anonymous enum")
    fun test02() = parsed("""
        typedef enum { Monday } MyEnum;
    """.trimIndent()) { ast ->
        assertEquals(1, ast.toplevelDeclarations.size)
    }


}