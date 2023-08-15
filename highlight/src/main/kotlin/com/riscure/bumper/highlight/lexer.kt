/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import com.riscure.bumper.highlight.lexer.CLexer
import java.io.Reader

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

    fun tokens(): List<Token> {
        val toks = mutableListOf<Token>()
        forEach { toks.add(it) }
        return toks
    }
}
