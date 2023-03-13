package com.riscure.bumper.sources

import arrow.core.Either
import com.riscure.bumper.ast.Exp
import com.riscure.bumper.ast.Stmt
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.pp.AstWriters
import com.riscure.bumper.pp.Printer
import java.io.Writer
import kotlin.io.path.nameWithoutExtension

sealed interface Preproc {
    fun pretty(): String

    data class Include(
        /** Excluding quotes, but including angular brackets if applicable */
        private val header: String
    ): Preproc {
        val quoted: String get() =
            if (header.isNotEmpty() && header.first() == '<') "\"$header\""
            else header

        override fun pretty() = "#include $quoted"
    }
}

data class Source(
    val cppHeader: List<Preproc>,
    val description: List<String>,
    val unit: TranslationUnit<Exp, Stmt>
) {
    val prettyHeader get() = cppHeader.joinToString(separator = "\n") { it.pretty() }
    val prettyDescription get() = description.joinToString(separator = "\n") { "// $it" }

    val path get() = unit.tuid.main
    val headerPath get() = "${unit.tuid.main.nameWithoutExtension}.h"

    fun write(writer: Writer): Either<String, Unit> {
        writer.write(prettyHeader)
        writer.write("\n")
        writer.write("\n")
        writer.write(prettyDescription)
        writer.write("\n")

        return AstWriters
            .usingPretty()
            .print(unit)
            .map { w -> w.write(Printer.of(writer)) }
    }
}