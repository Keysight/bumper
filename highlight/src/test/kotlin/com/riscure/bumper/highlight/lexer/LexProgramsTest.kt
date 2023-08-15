package com.riscure.bumper.highlight.lexer

import com.riscure.bumper.highlight.Token
import com.riscure.bumper.highlight.tokenize
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class LexProgramsTest {

    fun roundtrip(input: String) {
        val output = StringBuilder()
        tokenize(input.reader()).apply {
            while (hasNext()) {
                next().let { tok ->
                    println(tok)
                    output.append(tok.pp())
                }
            }
        }

        assertEquals(input, output.toString())
    }

    fun Iterator<Token>.flush() {
        while (hasNext()) { next() }
    }

    @Test
    @DisplayName("simple main")
    fun test_00() = roundtrip("""
        int main(const int argc, char **argv) {
            return 0;
        }
    """.trimIndent())

    @Test
    @DisplayName("multiline string")
    fun test_01() = roundtrip("""
        char *s = "begins here\
                   ends here";
    """.trimIndent())

    @Test
    @DisplayName("raw multiline string")
    fun test_02() = roundtrip("""
        char *s = R"(begins here
                     ends here)";
    """.trimIndent())

}