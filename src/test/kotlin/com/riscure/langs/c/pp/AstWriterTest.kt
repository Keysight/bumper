package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.langs.c.ast.Declaration
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.parser.clang.ClangParser
import java.nio.charset.Charset
import kotlin.io.path.*
import kotlin.test.*

internal class AstWriterTest {
    fun literal(input: String, transform: (unit: TranslationUnit) -> TranslationUnit = {it}): String {
        val file = createTempFile(suffix = ".c").apply {
            writeText(input)
        }

        val parser = ClangParser()
        val ast = parser.parse(file.toFile())
            .flatMap { it.ast() }
            .getOrHandle { throw it }

        val extractor = Extractor(file.toFile(), Charset.defaultCharset())
        fun bodyPrinter(tl : Declaration) =
            extractor.rhsOf(tl)

        return AstWriters { bodyPrinter(it) }
            .print(transform(ast))
            .getOrHandle { throw it }
            .write()
    }

    @Test
    fun main() {
        val input = """
            int main(int argc, char *argv[]) {
                int x;
            }
        """.trimIndent()

        assertEquals(input, literal(input))

    }

    @Test
    fun short() {
        val input = """
            int f(short s) {}
        """.trimIndent()

        assertEquals(input, literal(input))
    }

    @Test
    fun longlongPointer() {
        val input = """
            int f(long long *l) {}
        """.trimIndent()

        assertEquals(input, literal(input))
    }

    @Test
    fun varWithoutRhs() {
        val input = """
            int x;
        """.trimIndent()

        assertEquals(input, literal(input))
    }

    @Test
    fun varWithRhs() {
        val input = """
            int x = 42;
        """.trimIndent()

        assertEquals(input, literal(input))
    }


    @Test
    fun constSizeArray() {
        val input = """
            int xs[1] = { 42 };
            int ys[1][2];
            int zs[1][2][3];
            int *us[1][2][3];
        """.trimIndent()

        assertEquals(input, literal(input))
    }

    @Test
    fun arrayReturnType() {
        val input = """
            int *f();
            int **g();
            int ***h();
        """.trimIndent()

        val output = """
            int *f();
            int ***h();
        """.trimIndent()

        assertEquals(input, literal(input))
        assertEquals(
            output,
            literal(input) { ast -> ast.copy(decls = ast.decls.filter { it.name != "g" }) }
        )
    }

    @Test
    fun funToPrototype() {
        val input = """
            int g(int i) {}
            int f() {
               g(42);
            }
        """.trimIndent()

        val output = """
            int f();
        """.trimIndent()

        assertEquals(
            output,
            literal(input) { ast ->
                ast.copy(decls =
                    ast.decls
                        // remove g
                        .filter { it.name != "g" }
                        // turn f into a prototype
                        .map { tl ->
                            if (tl is Declaration.Fun && tl.name == "f") {
                                tl.copy(isDefinition = false)
                            } else tl
                        }
                )}
        )
    }

    @Test
    fun funPointer() {
        val input = """
            typedef void (*fptr)(int x);
        """.trimIndent()

        println(literal(input))
    }
}