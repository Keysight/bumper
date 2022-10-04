package com.riscure.langs.c

import arrow.core.*
import com.riscure.Fallable
import com.riscure.langs.c.analysis.Identity
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.parser.ClangParserTest
import com.riscure.langs.c.parser.UnitState
import java.io.File
import java.io.StringWriter
import java.nio.file.Path

import kotlin.test.*

internal class FrontendTest {
    private val storage = Storage.temporary("FrontendTest").getOrHandle { throw it }
    private val frontend = Frontend.clang(
        Path.of("clang"),
        Identity(),
        storage
    )

    private fun processed(resource: String, whenOk: (ast: TranslationUnit, state: UnitState) -> Unit) {
        val test = File(ClangParserTest::class.java.getResource(resource)!!.file)
        val result = frontend
            .process(test, listOf())
            .flatMap { it.ast().map{ ast -> Pair(ast, it) }}

        when (result) {
            is Either.Left  -> fail("Expected successful processing, got error: ${result.value}")
            is Either.Right -> whenOk(result.value.first, result.value.second)
        }
    }

    @Test
    fun simple() {
        val input = """
        int main() { }
        """.trimIndent()

        val inputFile = kotlin.io.path.createTempFile(suffix = ".c").toFile()
        inputFile.writeText(input, Charsets.US_ASCII)

        when (val result = frontend.process(inputFile, listOf())) {
            is Either.Left -> fail(result.value.message)
            is Either.Right -> println(result.value.ast())
        }

        inputFile.delete()
    }

    @Test
    fun test001() {
        processed("/parser-tests/001-minimal-main.c") { tu, unit ->
            val ds = tu.decls
            val writer = object: StringWriter(), Fallable by Fallable.tantrum {}

            unit.writeSource(ds, writer)
            assertEquals("\nint main() {\n}\n", writer.toString())
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
    }

}