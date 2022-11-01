package com.riscure.langs.c

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.index.Symbol
import com.riscure.langs.c.parser.UnitState
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

import kotlin.test.*

/**
 * C Declarations are strange. They can appear in many places.
 * We test some edge cases of frontend functionality for various declarations.
 */
internal class DeclarationsTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(
        Path.of("clang"),
        storage
    )

    private fun literal(
        input: String,
        opts: Options = listOf(),
        whenOk: (TranslationUnit, UnitState) -> Unit
    ) {
        val file: File = kotlin.io.path.createTempFile(suffix = ".c").apply { writeText(input) }.toFile()
        val result = frontend
            .process(file, opts)
            .flatMap { it.ast().map { ast -> Pair(ast, it) } }
        try {
            when (result) {
                is Either.Left -> fail("Expected successful processing, got error: ${result.value}")
                is Either.Right -> whenOk(result.value.first, result.value.second)
            }
        } finally {
            file.delete()
        }
    }

    /**
     * Global declarations can occur in a nested position in C.
     * In the following program, Inner is globally visible.
     */
    @Test
    fun namedNestedStruct() =
        literal("""
            struct Outer {
              struct Inner {
                int i;
              } inner;
            };
            
            void f() {
              struct Inner i = { .i = 0 };
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.getReferencedToplevels(f)).value
            assertContains(tls.map { it.name }, "Inner")

            assertContains(ast.symbols, Symbol.struct(ast.tuid, "Inner"))
            assertContains(ast.symbols, Symbol.struct(ast.tuid, "Outer"))
        }

    /**
     * ... and of course, an anonymous struct can have a named inner declaration:
     */
    @Test
    fun anonymousNestedWithNamedNestedStruct() =
        literal("""
            struct Outer {
              struct {
                struct MoreInner { int i; } moreInner;
              } inner;
            };
        """.trimIndent()) { ast , unit ->
            assertContains(ast.symbols, Symbol.struct(ast.tuid, "MoreInner"))
        }
}