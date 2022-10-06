package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.Writer

/**
 * The interface to the state of the parser. This negotiates between
 * the True Code representation of C programs (i.e., [TranslationUnit] and its siblings),
 * and the third-party parser representation of the same programs.
 * This interface is all that stands between us and the wild-west of
 * libclang, for example.
 *
 * Because parsers may be native libraries, this implements Closeable
 * and you have to promise to properly call close() when you're done with
 * the instance of UnitState.
 */
interface UnitState : Closeable {
    class NoSource(val name: String):
        Exception("Failed to get source for top-level declaration '$name'")

    /**
     * Return the top-level entities that the definition of the given
     * top-level entity for the refers to.
     */
    fun getReferencedToplevels(decl: TopLevel): Either<Throwable,Set<TopLevel>>

    /**
     * Given a top-level declaration *in the unit that this state belongs to*
     * we can reproduce the (preprocessed) source-code.
     *
     * If the top-level declaration does not come from the original source,
     * this will may fail, or worse: succeed but return the wrong source.
     */
    fun getSource(decl: TopLevel): Option<String>

    /**
     * Writes the source for a collection of toplevel declarations from this
     * translation unit.
     */
    fun <T> writeSource(ast: List<TopLevel>, writer: T) where T : Writer {
        ast.forEach { tld ->
            when (val src = getSource(tld)) {
                is Some -> {
                    val entry = src.value.trim()
                    if (!entry.isBlank()) writer.write(entry + ";\n")
                }
                None -> TODO() // writer.fail(NoSource(tld.name))
            }
        }
    }

    /**
     * Convert this translation unit to an AST.
     */
    fun ast(): Either<Throwable, TranslationUnit>
}