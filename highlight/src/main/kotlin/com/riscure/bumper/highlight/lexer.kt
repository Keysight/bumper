/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import com.riscure.bumper.highlight.lexer.CLexer
import java.io.Reader
import java.nio.file.Path
import kotlin.io.path.reader

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
                tok == null                -> throw LexerException(lexer.pos, lexer.yytext())
                else                       -> tok
            }
        }

    fun tokens(): List<Token> {
        val toks = mutableListOf<Token>()
        forEach { toks.add(it) }
        return toks
    }
}

fun main(args: Array<String>) =
    args
        .let {
            require(it.size > 0)
            Path.of(it[0])
        }
        .reader()
        .let {
            tokenize(it, true)
                .tokens()
                .forEach {
                    println(it)
                }
        }