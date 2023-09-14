package com.riscure.bumper.sources

import arrow.core.Either
import com.riscure.bumper.ast.Exp
import com.riscure.bumper.ast.Stmt
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.ast.UnitDeclaration
import com.riscure.bumper.pp.AstWriters
import com.riscure.dobby.clang.Include
import java.io.Writer
import kotlin.io.path.nameWithoutExtension

data class Preamble(
    val cppHeader: List<Include> = listOf(),
    val declarations: Collection<UnitDeclaration<Exp, Stmt>> = listOf()
) {
    /**
     * Prepend this preamble to [source].
     */
    operator fun plus(source: Source) = source.copy(
        cppHeader = cppHeader + source.cppHeader,
        unit = source.unit.copy(
            declarations = declarations + source.unit.declarations
        )
    )

    /**
     * Append [other] to this.
     */
    fun plus(other: Preamble) = copy(
        cppHeader + other.cppHeader,
        declarations + other.declarations
    )
}

data class Source(
    val cppHeader: List<Include> = listOf(),
    val description: List<String> = listOf(),
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
            .map { pp -> pp.writeTo(writer) }
    }
}