package com.riscure.bumper.parser

import arrow.core.*
import com.riscure.bumper.ast.Location
import com.riscure.dobby.clang.Options
import com.riscure.bumper.index.TUID
import java.io.File


enum class Severity {
    INFO,
    WARNING,
    ERROR;

    override fun toString() = when (this) {
        INFO    -> "info"
        WARNING -> "warning"
        ERROR   -> "error"
    }
}
data class Diagnostic(
    val severity: Severity,
    val loc: Location,
    val presumedLoc: Option<Location>,
    val cause: String
) {
    val headline get() = cause.lines().firstOrNone().getOrElse { "" }

    val details get()  =
        cause.lines().some()
            .filter { it.isEmpty() }
            .map { it.drop(1).joinToString { "\n" }}

    fun format(): String {
        val presumed = presumedLoc
            .map { "\n              originally at $it" } // indent is trimmed
            .getOrElse { "" }
        return """
            - $severity
              at $loc$presumed
              because: $headline
        """.trimIndent() + (details.map { "\n\n$${it.prependIndent("  ")}" } .getOrElse { "" })
    }
}

fun interface Parser<Exp, Stmt, out S : UnitState<Exp, Stmt>> {

    /**
     * Given a file, preprocess and parse it. As a side-effect, this may perform
     * static analysis of the file, so this can fail on illtyped C programs.
     *
     * Some options are removed, namely any option conflicting with -E,
     * and any option specifying output.
     */
    fun parse(file: File, opts: Options, tuid: TUID) : Either<ParseError, S>
    fun parse(file: File, opts: Options) : Either<ParseError, S> = parse(file, opts, TUID(file.toPath()))
    fun parse(file: File) : Either<ParseError, S> = parse(file, listOf())
}
