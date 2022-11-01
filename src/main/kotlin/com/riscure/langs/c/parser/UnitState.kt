package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.ast.Declaration
import com.riscure.langs.c.ast.TranslationUnit
import java.io.Closeable
import java.io.Writer

/**
 * The interface to the state of the parser. This negotiates between
 * the True Code representation of C programs (i.e., [TranslationUnit] and its siblings),
 * and the third-party parser representation of the same programs.
 *
 * It is parameterized by an opaque type of statements that make up definitions.
 *
 * This interface is all that stands between us and the wild-west of
 * libclang, for example.
 *
 * Because parsers may be native libraries, this implements Closeable
 * and you have to promise to properly call close() when you're done with
 * the instance of UnitState.
 */
interface UnitState: Closeable {
    class NoSource(val name: String):
        Exception("Failed to get source for top-level declaration '$name'")

    /**
     * Return the globally visible declarations that the given declaration refers to
     */
    fun getReferencedDeclarations(decl: TLID): Either<Throwable,Set<TLID>>

    /**
     * Convert this translation unit to an AST.
     */
    fun ast(): Either<Throwable, TranslationUnit>
}