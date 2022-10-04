package com.riscure.langs.c.parser

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.riscure.Fallable
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.Writer

/**
 * The interface of an abstract state representing a translation unit.
 */
interface UnitState : Closeable {
    class NoSource(val name: String):
        Exception("Failed to get source for top-level declaration '$name'")

    /**
     * Given a top-level declaration *in the unit that this state belongs to*
     * we can reproduce the (preprocessed) source-code.
     */
    fun getSource(decl: TopLevel): Option<String>

    /**
     * Writes the source for a collection of toplevel declarations from this
     * translation unit.
     */
    fun <T> writeSource(ast: List<TopLevel>, writer: T) where T : Writer, T : Fallable {
        ast.forEach { tld ->
            when (val src = getSource(tld)) {
                is Some -> {
                    val entry = src.value.trim()
                    if (!entry.isBlank()) writer.write(entry + ";\n")
                }
                None -> writer.fail(NoSource(tld.name))
            }
        }
    }

    /**
     * Convert this translation unit to an AST.
     */
    fun ast(): Either<Throwable, TranslationUnit>
}