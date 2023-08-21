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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.nio.file.Path
import kotlin.io.path.reader

object CoverageReportSerializer {

    val Token.typeEncoding get() = when (this) {
        is Token.Blob -> "b"
        is Token.CharLiteral -> "c"
        is Token.Directive -> "d"
        is Token.EOF -> "e"
        is Token.Identifier -> "i"
        is Token.IntLiteral -> "I"
        is Token.Keyword -> "k"
        is Token.Punctuation -> "p"
        is Token.StringLiteral -> "s"
        is Token.MultiLineComment -> "M"
        is Token.Type -> "t"
        is Token.Ws -> "w"
    }

    /**
     * Produces a __compact__ JSON serialization of a token.
     */
    fun Token.toJson(): JsonObject = buildJsonObject {
        put("t", JsonPrimitive(typeEncoding))
        put("pp", JsonPrimitive(pp()))
    }

    fun List<Segment>.toJson(): JsonArray = buildJsonArray {
        this@toJson.forEach { segment ->
            add(buildJsonObject {
                put(
                    "count",
                    JsonPrimitive(
                        segment.coverage
                            .getOrElse { null }
                            ?.count
                    )
                )

                put(
                    "tokens",
                    buildJsonArray {
                        segment.tokens.forEach { token ->
                            add(token.toJson())
                        }
                    }
                )
            })
        }
    }
}

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

enum class Mode {
    JSON, Print;
}

private val json = Json {
    this.prettyPrint = false // compact
}

fun main(args: Array<String>) {

    val (mode, cfile, coverageJson) = when {
        args.size < 2  -> error("expected arguments: file.c coverage.json")
        args.size == 2 -> Triple(Mode.Print, args[0], args[1])
        args.size == 3 && args[0].lowercase() == "json" -> Triple(Mode.JSON, args[1], args[2])
        else           -> error("invalid arguments")
    }

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
    when (mode) {
        Mode.JSON  -> with (CoverageReportSerializer) {
            Json
                .encodeToString(annotated.toJson())
                .let { println(it) }
        }

        Mode.Print -> annotated.ppToTerminal()
    }
}