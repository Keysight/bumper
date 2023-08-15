/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import arrow.core.getOrElse
import arrow.core.getOrHandle
import arrow.core.toOption
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextColors.Companion.rgb
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.terminal.Terminal
import com.riscure.bumper.coverage.Report
import java.nio.file.Path
import kotlin.io.path.reader

fun List<Segment>.ppToTerminal(
    term: Terminal = Terminal(),
    waterMarks: List<Pair<Int, TextStyle>> = listOf(
        0 to rgb("bc4442").bg + black,
        1 to rgb("e09123").bg + black,
        3 to rgb("e0c023").bg + black,
        7 to rgb("a8bc42").bg + black,
        15 to rgb("42bc6f").bg + black,
        Int.MAX_VALUE to rgb("42bcac").bg + black,
    )
) = forEach { segment ->
    val (tokens, coverage) = segment
    val bg = waterMarks
        .find { (mark, style) -> coverage.exists { it.count <= mark }}
        .toOption()
        .map { it.second }
        .getOrElse { TextStyle() }

    tokens.forEach { tok ->
        val hl: TextStyle = when (tok) {
            is Token.CharLiteral   -> brightGreen
            is Token.Directive     -> gray
            is Token.IntLiteral    -> blue
            is Token.Keyword       -> magenta
            is Token.StringLiteral -> green
            is Token.Type          -> red
            else -> TextStyle()
        }

        term.print((bg + hl)(tok.pp()))
    }
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        error("expected arguments: file.c coverage.json")
    }

    val cfile = args[0]
    val coverageJson = args[1]

    // get the coverage data for cfile
    val report = Report
        .deserialize(Path.of(coverageJson))
        .getOrHandle { error("Failed to deserialize coverage report: $it") }
    val fileReport = report.files
        .find { Path.of(it.path).fileName == Path.of(cfile).fileName }
        ?: error("No coverage data for file '$cfile' in '$coverageJson'.")

    // lex the c file
    val toks = tokenize(Path.of(cfile).reader().buffered())

    // combine the data
    val annotated = annotate(toks, fileReport.segments.map { it.marker })

    // render it
    annotated.ppToTerminal()
}