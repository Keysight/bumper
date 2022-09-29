package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.File

typealias Result<T> = Either<String, T>

/**
 * The interface of an abstract state representing a translation unit.
 */
interface UnitState : Closeable {
    /**
     * Given a top-level declaration *in the unit that this state belongs to*
     * we can reproduce the source-code.
     */
    fun getSource(decl: TopLevel): Option<String>

    /**
     * Generates the source for a set of toplevel declarations from this
     * translation unit.
     */
    fun getSource(ast: List<TopLevel>): Option<String> {
        val result = StringBuilder()

        ast.forEach { tld ->
            when (val src = getSource(tld)) {
                is Some -> {
                    result.append("\n")
                    result.append(src)
                }
                None -> return None
            }
        }

        return result.toString().some()
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
