package com.riscure.langs.c.parser

import arrow.core.Either
import com.riscure.langs.c.ast.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class ClangParserTest() {

    private fun parsed(resource: String, whenOk: (result: TranslationUnit) -> Unit) {
        val test = File(ClangParserTest::class.java.getResource(resource)!!.file)
        when (val parse = ClangParser().parse(test)) {
            is Either.Left -> fail("Expected successful parse, got error: ${parse.value}")
            is Either.Right -> whenOk(parse.value)
        }
    }

    @Test
    fun test001() {
        parsed("/parser-tests/001-minimal-main.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.FunDef)

            val fn = tu.decls[0] as TopLevel.FunDef
            assertEquals("main", fn.name)
        }
    }

    @Test
    fun test002() {
        parsed("/parser-tests/002-main-with-params.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.FunDef)

            val fn = tu.decls[0] as TopLevel.FunDef
            assertEquals("main", fn.name)

            assertEquals(2, fn.params.size)
            assertEquals("argc", fn.params[0].name)
            assertEquals(Type.Int(IKind.IInt), fn.params[0].type)
            assertEquals("argv", fn.params[1].name)
            assertEquals(Type.Array(Type.Ptr(Type.Int(IKind.IChar))), fn.params[1].type)
        }
    }

    @Test
    fun test003() {
        parsed("/parser-tests/003-function-decl.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.FunDecl)
        }
    }

    @Test
    fun test004() {
        parsed("/parser-tests/004-struct-decl.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test005() {
        parsed("/parser-tests/005-struct-def.c") { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test006() {
        parsed("/parser-tests/006-anonymous-struct.c") { tu ->
            assertEquals(2, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertEquals(StructOrUnion.Struct, (tu.decls[0] as TopLevel.Composite).structOrUnion)
        }
    }

    @Test
    fun test007() {
        parsed("/parser-tests/007-named-struct-with-var.c") { tu ->
            assertEquals(3, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)

            assertTrue(tu.decls[1] is TopLevel.VarDecl)
            val book = tu.decls[1] as TopLevel.VarDecl
            assertEquals(Type.Struct("Book"), book.type)

            assertTrue(tu.decls[2] is TopLevel.VarDecl)
            val another = tu.decls[2] as TopLevel.VarDecl
            assertEquals(Type.Struct("Book"), another.type)

        }
    }

    @Test
    fun test008() {
        parsed("/parser-tests/008-typedef.c") { tu ->
            assertEquals(3, tu.decls.size)
            assertTrue(tu.decls[0] is TopLevel.Composite)
            assertTrue(tu.decls[1] is TopLevel.Typedef)

            assertTrue(tu.decls[2] is TopLevel.VarDecl)
            val another = tu.decls[2] as TopLevel.VarDecl
            assertEquals(Type.Named("mybook", Type.Struct("Book")), another.type)
        }
    }

}