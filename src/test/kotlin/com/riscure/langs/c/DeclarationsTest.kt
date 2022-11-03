package com.riscure.langs.c

import arrow.core.*
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.parser.clang.ClangTranslationUnit
import com.riscure.langs.c.parser.clang.ClangUnitState
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
        whenOk: (ClangTranslationUnit, ClangUnitState) -> Unit
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

    @Test
    fun simpleLocalReference() =
        literal("""
            int main() {
              int x;
              return x;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "main" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertEquals(0, tls.size)
        }


    @Test
    fun structReferenceInType() =
        literal("""
            struct S {};
            int main() {
              struct S *s;
              return 0;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "main" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertContains(tls.map { it.name }, "S")
            assertEquals(1, tls.size)
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
              struct Inner inner = { .i = 0 };
              inner.i = 1;
              inner = inner;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertContains(tls.map { it.name }, "Inner")
            assertEquals(1, tls.size)
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
            
            void f() {
              struct MoreInner inner = { .i = 0 };
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertContains(tls.map { it.name }, "MoreInner")
            assertEquals(1, tls.size)
        }

    /**
     * We can depend on a typedef:
     */
    @Test
    fun typedef() =
        literal("""
            typedef struct S {} MyS;
            
            void f() {
              MyS i;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertContains(tls.map { it.name }, "MyS")
            assertEquals(1, tls.size)
        }

    /**
     * ... or on the declaration of a type within a typedef.
     */
    @Test
    fun structDeclInTypedef() =
        literal("""
            typedef struct S {} MyS;
            
            void f() {
              struct S s;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertContains(tls.map { it.name }, "S")
            assertEquals(1, tls.size)
        }

    /**
     * ... but we can't see local typedefs.
     */
    @Test
    fun localTypedef() =
        literal("""
            void f() {
              typedef struct S {} MyS;
              MyS s;
            }
        """.trimIndent()) { ast , unit ->
            val f = assertNotNull(ast.functions.find { it.name == "f" })

            val tls = assertIs<Either.Right<Set<TLID>>>(unit.ofDecl(f)).value
            assertEquals(0, tls.size)
        }
}