package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.Failable
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.File
import java.io.Writer

typealias Result<T> = Either<String, T>

/**
 * The interface of an abstract state representing a translation unit.
 */
interface UnitState : Closeable {
    class NoSource(val name: String): Exception("Failed to get source for top-level declaration '$name'")

    /**
     * Given a top-level declaration *in the unit that this state belongs to*
     * we can reproduce the source-code.
     */
    fun getSource(decl: TopLevel): Option<String>

    /**
     * Writes the source for a collection of toplevel declarations from this
     * translation unit.
     */
    fun <T> writeSource(ast: List<TopLevel>, writer: T) where T : Writer, T : Failable {
        ast.forEach { tld ->
            when (val src = getSource(tld)) {
                is Some -> {
                    writer.write("\n")
                    writer.write(src.value)
                }
                None -> writer.fail(NoSource(tld.name))
            }
        }
    }

    /**
     * Convert this translation unit to an AST.
     */
    fun ast(): Result<TranslationUnit>
}

interface Parser<S : UnitState> {
    /**
     * Given a file, parse it.
     */
    fun parse(file: File) : Result<S>
}
