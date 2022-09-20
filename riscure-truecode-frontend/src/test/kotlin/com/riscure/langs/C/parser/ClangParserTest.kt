package com.riscure.langs.C.parser

import arrow.core.Either
import com.riscure.langs.c.*
import com.riscure.tc.codeanalysis.clang.compiler2.loader.CompileCommandMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class ClangParserTest() {

    private fun parsed(f: File, whenOk: (result: TranslationUnit) -> Unit) {
        when (val parse = ClangParser().parse(f)) {
            is Either.Left -> fail("Expected successful parse, got error: ${parse.value}")
            is Either.Right -> whenOk(parse.value)
        }
    }
    /**
     * Minimal main function parse
     */
    @Test
    fun parseExample01() {
        val c01 = File(ClangParserTest::class.java.getResource("/examples/ex01.c")!!.file)
        parsed(c01) { tu ->
            assertEquals(1, tu.decls.size)
            assertTrue(tu.decls[0] is GlobalDecl.Fun)

            val fn = tu.decls[0] as GlobalDecl.Fun
            assertEquals("main", fn.name)

            assertEquals(2, fn.params.size)
            assertEquals("argc", fn.params[0].name)
            assertEquals(Type.Int(IKind.IInt), fn.params[0].type)
            assertEquals("argv", fn.params[1].name)
            assertEquals(Type.Array(Type.Ptr(Type.Int(IKind.IChar))), fn.params[1].type)
        }
    }

    @Test
    fun parseExample02() {
        val c02 = File(ClangParserTest::class.java.getResource("/examples/ex02.c")!!.file)
        parsed(c02) { tu ->
            assertTrue(tu.decls[0] is GlobalDecl.Fun)

            val fn = tu.decls[0] as GlobalDecl.Fun
            assertTrue(fn.body is Stmt.Block)

            val b = fn.body as Stmt.Block
            assertEquals(2, b.stmts.size)

            val s1 = b.stmts[0]
            val s2 = b.stmts[1]
            assertTrue(s1 is Stmt.Decl)
            assertTrue(s2 is Stmt.Decl)

            val d1 = s1 as Stmt.Decl
            val d2 = s2 as Stmt.Decl
            assertEquals("i", d1.name)
            assertEquals(Type.Int(IKind.IInt), d1.type)
            assertEquals("array", d2.name)
            assertEquals(Type.Array(Type.Int(IKind.IInt), Optional.of(8)), d2.type)
        }
    }
}