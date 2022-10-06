package com.riscure.langs.c.parser.clang

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.right
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.ast.functions
import com.riscure.langs.c.parser.asVarDecl
import java.io.File
import kotlin.test.*

internal class ClangUnitStateTest {

    private fun parsed(resource: String, whenOk: (ast: TranslationUnit, state: ClangUnitState) -> Unit) {
        val test = File(javaClass.getResource(resource)!!.file)
        ClangParser().parse(test).tap { it.use { unit ->
            when (val ast = unit.ast()) {
                is Either.Left -> fail("Expected successful parse, got error: ${ast.value}")
                is Either.Right -> whenOk(ast.value, unit)
            }
        }}
    }

    @Test
    fun test01() = parsed("/analysis-tests/001-references-in-function.c") { ast, unit ->
        val main = ast.decls
            .functions()
            .filter { it.name == "main" }
            .get(0)!!

        println(unit.getReferencedToplevels(main))
    }

    @Test
    fun test02() = parsed("/analysis-tests/002-references-in-global.c") { ast, unit ->
        val main = ast.decls
            .filter { it.name == "x" }
            .get(0)!!

        // a enumerator is not a top-level declaration, technically.
        // so this is right
        assertEquals(setOf<TopLevel>().right(), unit.getReferencedToplevels(main))
    }

    @Test
    fun test03() = parsed("/analysis-tests/003-void-pointers.c") { ast, unit ->
        val ptrs = ast.decls
            .filter { it.name == "pointers" }
            .get(0)!!

        when (val refs = unit.getReferencedToplevels(ptrs)) {
            is Either.Right -> {
                assertEquals(3, refs.value.size)
                val tls = refs.value.map { it.name }

                assertContains(tls, "f")
                assertContains(tls, "g")
                assertContains(tls, "funcptr")
            }
            else -> fail()
        }
    }

    @Test
    fun test04() = parsed("/analysis-tests/004-void-pointers-with-args.c") { ast, unit ->
        val ptrs = ast.decls
            .filter { it.name == "pointers" }[0]!!

        when (val refs = unit.getReferencedToplevels(ptrs)) {
            is Either.Right -> {
                assertEquals(3, refs.value.size)
                val tls = refs.value.map { it.name }

                assertContains(tls, "f")
                assertContains(tls, "g")
                assertContains(tls, "funcptr")
            }
            else -> fail()
        }
    }

    @Test
    fun test05() = parsed("/analysis-tests/005-cursor-of-global-var.c") { ast, unit ->
        val v = ast.decls[0]!!
        with (unit) {
            val c = v.getCursor().getOrElse { fail() }
            // check that getCursor return the VarDecl
            assertTrue(c.asVarDecl() is Either.Right)
        }
    }

    @Test
    fun test06() = parsed("/analysis-tests/006-typedef-in-return-type.c") { ast, unit ->
        val fn = ast.decls.filter{ it.name == "f" }[0]!!
        val refs = assertIs<Either.Right<Set<TopLevel>>>(unit.getReferencedToplevels(fn))
        assertEquals(1, refs.value.size)
    }
}