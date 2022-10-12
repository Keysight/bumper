package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import com.riscure.langs.c.parser.clang.ClangParser
import java.io.StringWriter
import kotlin.io.path.*
import kotlin.test.*

internal class AstWriterTest {
    fun literal(input: String, transform: (unit: TranslationUnit) -> TranslationUnit = {it}): String {
        val s = StringWriter()

        val file = createTempFile(suffix = ".c").apply {
            writeText(input)
        }

        val parser = ClangParser()
        val ast = parser.parse(file.toFile())
            .flatMap { it.ast() }
            .getOrHandle { throw it }

        val bodyPrinter: (tl : TopLevel) -> Writer = { empty }
        AstWriters(bodyPrinter)
            .print(transform(ast))
            .invoke { data -> s.write(data) }

        return s.toString()
    }

    @Test
    fun main() {
        val input = """
            int main(int argc, char* argv[]) {}
        """.trimIndent()

        println(literal(input))
    }

    @Test
    fun short() {
        val input = """
            int f(short s) {}
        """.trimIndent()

        println(literal(input))
    }

    @Test
    fun longlongPointer() {
        val input = """
            int f(long long *l) {}
        """.trimIndent()

        println(literal(input))
    }


    @Test
    fun constSizeArray() {
        val input = """
            int xs[1];
            int ys[1][2];
            int zs[1][2][3];
        """.trimIndent()

        println(literal(input))
    }
}