package com.riscure.bumper.highlight.lexer

import com.riscure.bumper.highlight.Token
import com.riscure.bumper.highlight.tokenize
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.Reader
import java.net.URL
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals

class LexProgramsTest {
    fun tokenizeTest(input: Reader): String {
        val output = StringBuilder()
        tokenize(input).apply {
            while (hasNext()) {
                next().let { tok ->
                    println(tok)
                    output.append(tok.pp())
                }
            }
        }

        return output.toString()
    }

    fun roundtrip(input: String) {
        val output = tokenizeTest(input.reader())
        assertEquals(input, output.toString())
    }

    @ParameterizedTest(name = "roundtrip {0}")
    @MethodSource("programs")
    fun roundtripTest(name: String, program: String) = roundtrip(program)

    @ParameterizedTest(name = "regression test {0}")
    @MethodSource("regressions")
    fun regressionTest(name: String, file: URL): Unit {
        tokenizeTest(file.openStream().reader())
    }

    companion object {
        private fun regression(name: String) =
            name to Companion::class.java.classLoader.getResource("regression-tests/$name.c")!!


        @JvmStatic
        fun regressions(): Stream<Arguments> = listOf(
            regression("01")
        )
            .map { (name, url) -> Arguments.of(name, url) }
            .stream()

        @JvmStatic
        fun programs(): Stream<Arguments> = listOf(
            "simple main" to """
                int main(const int argc, char **argv) {
                    return 0;
                }
            """.trimIndent(),

            "multiline string" to """
                char *s = "begins here\
                           ends here";
            """.trimIndent(),

            "raw multiline string" to """
                char *s = R"(begins here
                ends here)";
            """.trimIndent(),

            "single-line block comment" to """
                /* block comment */
            """.trimIndent(),

            "multi-line block comment" to """
                /* block 
                   comment */
            """.trimIndent(),

            "multi-line doc comment" to """
                /** 
                  * block 
                  * comment 
                  */
            """.trimIndent(),

            "multi-line comment containg double-quotes" to """
                /** 
                  * "block"
                  * comment 
                  */
            """.trimIndent(),

            "multi-line comment containing single-quotes" to """
                /** 
                  * block's
                  * comment 
                  */
            """.trimIndent(),

            "multi-line block comment with escaped end" to """
                /* block *\/
                   comment */
            """.trimIndent(),
        )
            .map { (name, program) -> Arguments.of(name, program) }
            .stream()
    }
}