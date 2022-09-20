package com.riscure.langs.C.parser

import arrow.core.Either
import com.riscure.langs.c.ClangParser
import com.riscure.langs.c.GlobalDecl
import com.riscure.langs.c.IKind
import com.riscure.langs.c.Type
import com.riscure.tc.codeanalysis.clang.compiler2.loader.CompileCommandMapping
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class ClangParserTest() {
    /**
     * Minimal main function parse
     */
    @Test
    fun parseExamples() {
        val c01 = File(ClangParserTest::class.java.getResource("/examples/ex01.c")!!.file)
        when (val parse = ClangParser().parse(c01)) {
            is Either.Left -> fail("Expected successful parse")
            is Either.Right -> {
                val tu = parse.value
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
    }
}