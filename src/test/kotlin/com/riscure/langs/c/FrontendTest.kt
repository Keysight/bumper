package com.riscure.langs.c

import arrow.core.*
import com.riscure.dobby.clang.Arg
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.ast.functions
import com.riscure.langs.c.parser.clang.ClangTranslationUnit
import com.riscure.langs.c.parser.clang.ClangUnitState
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

import kotlin.test.*

internal class FrontendTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(
        Path.of("clang"),
        storage
    )

    private fun literal(
        input: String,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit
    ) {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(input) } .toFile()
        processed(file, opts, whenOk)
    }

    private fun processed(
        resource: String,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit
    ) {
        val test = File(this.javaClass.getResource(resource)!!.file)
        processed(test, opts, whenOk)
    }

    private fun processed(
        test: File,
        opts: Options = listOf(),
        whenOk: (ast: ClangTranslationUnit, state: ClangUnitState) -> Unit
    ) {
        val result = frontend
            .process(test, opts)
            .flatMap { it.ast.map{ ast -> Pair(ast, it) }}

        when (result) {
            is Either.Left  -> fail("Expected successful processing, got error: ${result.value}")
            is Either.Right -> whenOk(result.value.first, result.value.second)
        }
    }

    @Test
    fun simple() {
        val input = """int main() { }"""

        val inputFile = kotlin.io.path.createTempFile(suffix = ".c").toFile()
        inputFile.writeText(input)

        val unit = assertIs<Either.Right<ClangUnitState>>(frontend.process(inputFile, listOf()))
        val ast  = assertIs<Either.Right<ClangTranslationUnit>>(unit.value.ast)

        inputFile.delete()
    }

    @Test
    fun simpleWithTrailingSemicolon() {
        val input = """int main() { };\n"""

        val inputFile = kotlin.io.path.createTempFile(suffix = ".c").toFile()
        inputFile.writeText(input)

        // This is actually a parse error
        val unit = assertIs<Either.Left<*>>(frontend.process(inputFile, listOf()))
        inputFile.delete()
    }

    /**
     * This tests a useful invariant. The following square commutes:
     *
     *    source ------ clange -E ---------> B
     *      |                                |
     *   process           o              process
     *      |                                |
     *      A ----------- id --------------> ast minus metadata/prettyprinted source
     *
     */
    /* Disabled not working with maven?
    @Disabled("The preprocessed file is incompatible with windows (typedef size_t)")
    @Test
    fun test010() {
        processed("/parser-tests/010-demo-functions.c") { tu, unit ->
            val ds = tu.decls

            processed("/parser-tests/011-preprocessed-demo.c") { tup, unitp ->
                val dsp = tup.decls

                assertTrue(dsp.size > 50, "Expected a bunch of declarations")
                assertEquals(dsp.size, ds.size)

                val zipped = ds.zip(dsp)
                for (el in zipped) {
                    val (got, expected) = el

                    // compare the asts
                    // overwrite metadata, because those will differ
                    assertEquals(expected, got.withMeta(expected.meta))

                    // compare the pretty-printed sources
                    assertEquals(unitp.getSource(expected), unit.getSource(got))
                }
            }
        }
    }*/

    @Test
    fun missingInclude() {
        val test = File(javaClass.getResource("/parser-tests/015-include.c")!!.file)
        val result = frontend.process(test, listOf())

        assertTrue(result.isLeft(), "Expected processing failure due to missing include.")
    }

    @Test
    fun test015() { // include directives
        val includesDir = File(javaClass.getResource("/parser-tests/includes/")!!.file)
        val include = Arg.reads("-I" + includesDir.absolutePath).getOrElse { fail() }
        processed("/parser-tests/015-include.c", listOf(include)) { tu, _ ->
            println(tu)
        }
    }

    @Test
    fun testPrintf() = literal(
        """
            #include <stdio.h>
            void test() {
                printf("hi");
            }
        """.trimIndent()
    ) { ast, unit ->
        val main = ast.toplevelDeclarations.functions.filter { it.name == "test" }[0]!!
        val refs = assertIs<Either.Right<Set<TLID>>>(unit.dependencies.ofDecl(main))
        val tls = refs.value.map { it.name }
        assertContains(tls, "printf")
    }
}