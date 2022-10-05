package com.riscure.langs.c.preprocessor.clang

import com.riscure.langs.c.parser.ClangParserTest
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.forEachLine

internal class ClangPreprocessorTest {

    val cpp = ClangPreprocessor(Path.of("clang"))

    @Test
    fun simple() {
        // preprocessing a file without directives
        val test = File(ClangParserTest::class.java.getResource("/parser-tests/001-minimal-main.c")!!.file)
        val output = kotlin.io.path.createTempFile()
        cpp.preprocess(test, listOf(), output.toFile())
        output.forEachLine {
            println(it + "\n")
        }
    }
}