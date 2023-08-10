/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import arrow.core.Option
import com.riscure.bumper.highlight.lexer.CLexer
import java.io.Reader

data class StartSegmentMark(
    val line: Int,
    val column: Int,
    val count: Option<Int>,
)

class UnrecognizedInput(): Exception("Unrecognized input.")
fun tokenize(input: Reader, skipWhitespace: Boolean = false) =
    Tokenizer(CLexer(input), skipWhitespace)

class Tokenizer(
    val lexer: CLexer,
    val skipWs: Boolean
): Iterator<Token> {
    var finished = false

    override fun hasNext(): Boolean = !finished

    override fun next(): Token = lexer
        .yylex()
        .let { tok: Token? ->
            when {
                tok is Token.EOF           -> {
                    finished = true
                    tok
                }
                skipWs && tok is Token.Ws  -> next()
                tok == null                -> throw UnrecognizedInput()
                else                       -> tok
            }
        }
}
