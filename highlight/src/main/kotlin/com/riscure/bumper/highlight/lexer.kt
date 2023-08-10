/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import arrow.core.Option
import com.riscure.bumper.highlight.lexer.CLexer

data class StartSegmentMark(
    val line: Int,
    val column: Int,
    val count: Option<Int>,
)

fun main() {
    val input = """
        hello
    """.trimIndent().reader()

    val lexer = CLexer(input)

    while (true) {
        val tok = lexer.yylex()

        when {
            tok is Token.EOF -> break
            tok == null      -> break
            else             -> println(tok)
        }
    }
}
//data class Lexer(
//    var lineno: Int,
//    var colno: Int,
//
//    var cursor: Int,
//    var input: String,
//
//    val segmentQueue: MutableList<StartSegmentMark>
//) {
//    val next get() = input.elementAtOrNull(cursor)
//
//    companion object {
//        fun lexer(input: String): Either<String, List<Line>> {
//            TODO()
//        }
//    }
//}
//
