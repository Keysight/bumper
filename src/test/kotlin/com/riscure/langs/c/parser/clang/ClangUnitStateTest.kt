package com.riscure.langs.c.parser.clang

import kotlin.test.*
import arrow.core.*
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.*
import java.io.File

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

        // This now fails, because getting the cursor for
        // a top-level var declaration fails.
        // It gets the cursor for the type of the var declaration,
        // not the declaration itself.
        println(ptrs)
        println(unit.getReferencedToplevels(ptrs))
        fail()
    }

    @Test
    fun test04() = parsed("/analysis-tests/004-void-pointers-with-args.c") { ast, unit ->
        val ptrs = ast.decls
            .filter { it.name == "pointers" }
            .get(0)!!

        // This now fails, because getting the cursor for
        // a top-level var declaration fails.
        // It gets the cursor for the type of the var declaration,
        // not the declaration itself.
        println(unit.getReferencedToplevels(ptrs))
        fail()
    }
}