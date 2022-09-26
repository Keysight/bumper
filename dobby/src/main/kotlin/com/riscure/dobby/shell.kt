package com.riscure.dobby

import com.riscure.lang.shell.ShellLexer
import com.riscure.lang.shell.ShellParser.*
import com.riscure.lang.shell.ShellParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

data class Commandline(val parts: List<String>) {
    fun toShell() {
        parts.joinToString(separator = " ") { Shell.escape(it) }
    }

    companion object {
        fun ofArguments(parts: List<String>): Commandline =
            Commandline(parts)

        fun ofShell(cmd: String): Commandline {
            TODO()
        }

        fun ofShell(escapedParts: List<String>): Commandline =
            Commandline(escapedParts.map { Shell.unescape(it) })
    }
}

/**
 * Utilities to parse and transform a subset of shell encountered
 * in compilation databases.
 */
object Shell {

    fun unescape(s: String): String {
        TODO()
    }

    fun escape(s: String): String {
        TODO()
    }

    fun line(line: String): List<String> {
        val s = CharStreams.fromString(line)
        val parser = ShellParser(CommonTokenStream(ShellLexer(s)))
        val result = parser.line()

        return result.ast()
    }

    fun arg(line: String): Pair<String, String> {
        val s = CharStreams.fromString(line)
        val parser = ShellParser(CommonTokenStream(ShellLexer(s)))
        val result = parser.arg()

        // TODO the unwind is not super performant
        return Pair(result.text, s.unwind().trim())
    }

    private fun ShellParser.LineContext.ast(): List<String> =
        this.shellargs().ast()

    private fun ShellParser.ShellargsContext.ast(): List<String> = when (this) {
        is ConsContext   -> tail.ast().toMutableList().apply { add(0, head.ast()) }
        is SingleContext -> listOf(single.ast())
        is NilContext    -> listOf()
        else -> throw RuntimeException("This should never happen. Please report a bug.")
    }

    private fun ArgContext.ast(): String =
        `val`().joinToString(separator="") { it.text }

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