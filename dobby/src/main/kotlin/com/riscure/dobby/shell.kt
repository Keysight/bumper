package com.riscure.dobby

import arrow.core.*
import com.riscure.lang.shell.ShellLexer
import com.riscure.lang.shell.ShellParser.*
import com.riscure.lang.shell.ShellParser
import org.antlr.v4.runtime.*

typealias Result<T> = Either<Throwable,T>

/**
 * A symbol is an abstraction of a char in the context of possible
 * quotation. Escaped characters are represented as a single symbol.
 */
data class Symbol(val value: String) {
    companion object {
        val escapedDouble = Symbol("\\\"")
        val escapedSingle = Symbol("\\'")
        val escapedEscape = Symbol("\\\\")
        val escapedSpace  = Symbol("\\ ")
    }
    fun eval(): String = when (this) {
        escapedDouble -> "\""
        escapedSingle -> "'"
        escapedEscape -> "\\"
        escapedSpace  -> " "
        else          -> value
    }
}

data class Line(val args: List<Arg>) {
    fun eval(): List<String> = args.map { it.eval() }
}
data class Arg(val parts: List<Val>) {
    fun eval(): String = parts.joinToString(separator="") { it.eval() }
}

sealed class Val {
    abstract val content: List<Symbol>
    fun eval(): String = content.joinToString(separator="") { it.eval() }

    data class DoubleQuoted(override val content: List<Symbol>) : Val()
    data class SingleQuoted(override val content: List<Symbol>) : Val()

    data class Unquoted(override val content: List<Symbol>) : Val()
}

/**
 * Utilities to parse and transform a subset of shell encountered
 * in compilation databases.
 */
object Shell {

    /**
     * Parse the whole input as a commandline line.
     */
    fun line(line: String): Result<Line> {
        val s = CharStreams.fromString(line)
        val parser = ShellParser(CommonTokenStream(ShellLexer(s)))
        val result = parser.line()

        return result.ast()
    }

    /**
     * Parse a single argument from the string, and return the result
     * and the unparsed remainder.
     */
    fun arg(line: String): Result<Pair<Arg, String>> {
        val s = CharStreams.fromString(line)
        val parser = ShellParser(CommonTokenStream(ShellLexer(s)))
        val result = parser.arg()

        // TODO the unwind is not super performant
        return result.ast().map { Pair(it, s.unwind().trim()) }
    }

    private fun ShellParser.LineContext.ast(): Result<Line> =
        this.shellargs().ast().map { Line(it) }

    private fun ShellParser.ShellargsContext.ast(): Result<List<Arg>> = when (this) {
        is ConsContext   -> tail
            .ast()
            .flatMap { args ->
                head.ast().map { head -> args.toMutableList().apply { add(0, head) } }
            }
        is SingleContext -> single.ast().map { listOf(it) }
        is NilContext    -> listOf<Arg>().right()
        else -> escalate()
    }

    private fun <T> ParserRuleContext.escalate(): Result<T> =
        if (exception == null)
            throw RuntimeException("This should never happen. Please report a bug.")
        else exception.left()

    private fun ArgContext.ast(): Result<Arg> =
        `val`()
            .map { it.ast() }
            .sequenceEither()
            .map { Arg(it) }

    private fun ValContext.ast(): Result<Val> = when (this) {
        is SingleQuotedContext -> Val.SingleQuoted(CHAR().map { Symbol(it.text) }).right()
        is DoubleQuotedContext -> Val.DoubleQuoted(CHAR().map { Symbol(it.text) }).right()
        is UnquotedContext     -> Val.Unquoted(CHAR().map { Symbol(it.text) }).right()
        else -> escalate()
    }

    /**
     * Utility to unwind the remainder of a charstream onto a string.
     */
    private fun CharStream.unwind(): String {
        val s = java.lang.StringBuilder()
        while (true) {
            val c = LA(1)
            if (c != CharStream.EOF) {
                s.appendCodePoint(c)
                consume()
            } else break
        }
        return s.toString()
    }
}