package com.riscure.langs.c

import arrow.core.*
import com.riscure.Fallable
import com.riscure.dobby.clang.Arg
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.ast.functions
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.clang.ClangUnitState
import org.junit.jupiter.api.Disabled
import java.io.File
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.writeText

import kotlin.test.*

internal class FrontendTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(
        Path.of("clang"),
        storage
    )

    private fun literal(input: String, opts: Options = listOf(), whenOk: (ast: TranslationUnit, state: UnitState) -> Unit) {
        val file: File = kotlin.io.path.createTempFile(suffix=".c").apply { writeText(input) } .toFile()
        processed(file, opts, whenOk)
    }

    private fun processed(resource: String, opts: Options = listOf(), whenOk: (ast: TranslationUnit, state: UnitState) -> Unit) {
        val test = File(this.javaClass.getResource(resource)!!.file)
        processed(test, opts, whenOk)
    }

    private fun processed(test: File, opts: Options = listOf(), whenOk: (ast: TranslationUnit, state: UnitState) -> Unit) {
        val result = frontend
            .process(test, opts)
            .flatMap { it.ast().map{ ast -> Pair(ast, it) }}

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

        val unit = assertIs<Either.Right<UnitState>>(frontend.process(inputFile, listOf()))
        val ast  = assertIs<Either.Right<TranslationUnit>>(unit.value.ast())

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

    @Test
    fun test001() {
        processed("/parser-tests/001-minimal-main.c") { tu, unit ->
            val ds = tu.decls
            val writer = object: StringWriter(), Fallable by Fallable.tantrum {}

            unit.writeSource(ds, writer)
            assertEquals("int main() {\n};\n", writer.toString())
        }
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
        processed("/parser-tests/015-include.c", listOf(include)) { tu, unit ->
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
        val main = ast.decls.functions().filter { it.name == "test" }
        println(unit.getReferencedToplevels(main[0]))
    }

}