package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.ast.functions
import java.io.File
import kotlin.io.path.writeText
import kotlin.test.*

internal class ClangUnitStateTest {

    private fun literal(cstring: String, whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit) {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(cstring) } .toFile()
        parsed(file, whenOk)
    }

    private fun parsed(resource: String, whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit) {
        val test = File(javaClass.getResource(resource)!!.file)
        parsed(test, whenOk)
    }

    private fun parsed(test: File, whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit) {
        when (val unit = ClangParser().parse(test)) {
            is Either.Left -> fail("Expected successful parse, got error: ${unit.value}")
            is Either.Right -> when (val ast = unit.value.ast()) {
                is Either.Left -> fail("Expected successful parse, got error: ${ast.value}")
                is Either.Right -> whenOk(ast.value , unit.value)
            }
        }
    }

    @Test
    fun test01() = literal("""
        void f() {}
        struct s {};
        int main() {
          struct s x;
          f();
        }
    """.trimIndent()
    ) { ast, unit ->
        val main = ast.decls
            .functions()
            .filter { it.name == "main" }[0]!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(main))
        assertEquals(2, refs.value.size)
        val tls = refs.value.map { it.name }

        assertContains(tls, "f")
        assertContains(tls, "s")
    }

    @Test
    fun test02() = parsed("/analysis-tests/002-references-in-global.c") { ast, unit ->
        val main = ast.variables
            .filter { it.name == "x" }
            .get(0)!!

        // an enumerator is not a top-level declaration, technically.
        // so this is right
        assertEquals(setOf<TLID>().right(), unit.ofDecl(main))
    }

    @Test
    fun test03() = parsed("/analysis-tests/003-void-pointers.c") { ast, unit ->
        val ptrs = ast.variables
            .filter { it.name == "pointers" }
            .get(0)!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(ptrs))
        assertEquals(3, refs.value.size)
        val tls = refs.value.map { it.name }

        assertContains(tls, "f")
        assertContains(tls, "g")
        assertContains(tls, "funcptr")
    }

    @Test
    fun test04() = parsed("/analysis-tests/004-void-pointers-with-args.c") { ast, unit ->
        val ptrs = ast.variables
            .filter { it.name == "pointers" }[0]!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(ptrs))
        assertEquals(3, refs.value.size)
        val tls = refs.value.map { it.name }

        assertContains(tls, "f")
        assertContains(tls, "g")
        assertContains(tls, "funcptr")
    }

    @Test
    fun test06() = parsed("/analysis-tests/006-typedef-in-return-type.c") { ast, unit ->
        val fn = ast.decls.filter{ it.ident == "f".some() }[0]!!
        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(fn))
        assertEquals(1, refs.value.size)
    }

    @Test
    fun test07() = parsed("/analysis-tests/007-void-pointers-with-struct-args.c") { ast, unit ->
        val ptrs = ast.variables
            .filter { it.name == "pointers" }[0]!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(ptrs))
        assertEquals(3, refs.value.size)
        val tls = refs.value.map { it.name }

        assertContains(tls, "f")
        assertContains(tls, "g")
        assertContains(tls, "S")
    }

    @Test
    fun test08() = parsed("/analysis-tests/008-switch-dependency.c") { ast, unit ->
        val ptrs = ast.variables
            .filter { it.name == "func_switch" }[0]!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(ptrs))
        val tls = refs.value.map { it.name }

        assertContains(tls, "func_a")
        assertContains(tls, "func_b")
    }

    @Test
    fun test09() = parsed("/analysis-tests/009-switch-dependency.c") { ast, unit ->
        val ptrs = ast.functions
            .filter { it.name == "func_switch" }[0]!!

        val refs = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(ptrs))
        val tls = refs.value.map { it.name }

        assertContains(tls, "func_a")
        assertContains(tls, "func_b")
    }

    @Test
    fun test10() = literal("""
        typedef struct { int __val[2]; } __fsid_t;
        typedef __fsid_t fsid_t;
    """.trimIndent()) { ast, unit ->
        val fsid = assertNotNull(ast.structs.find { it.ident == "fsid_t".some() })
        val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(fsid)).value.map { it.name }
        assertContains(tls, "__fsid_t")
    }
}