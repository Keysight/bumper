package com.riscure.bumper.parser

import arrow.core.*
import com.riscure.bumper.analyses.UnitDependencyAnalysis
import com.riscure.bumper.ast.*
import java.io.Closeable

/**
 * The interface to the state of the parser. This negotiates between
 * the True Code representation of C programs (i.e., [ErasedTranslationUnit] and its siblings),
 * and the third-party parser representation of the same programs.
 *
 * It is parameterized by the types of expressions and statements that make up definitions.
 *
 * This interface is all that stands between us and the wild-west of
 * libclang, for example.
 *
 * Because parsers may be native libraries, this implements Closeable
 * and you have to promise to properly call close() when you're done with
 * the instance of UnitState.
 */
interface UnitState<Exp,Stmt>: Closeable {
    class NoSource(val name: String):
        Exception("Failed to get source for top-level declaration '$name'")

    /**
     * Convert this translation unit to an AST.
     */
    val ast: Either<Throwable, TranslationUnit<Exp, Stmt>>

    /**
     * The dependency analyzer.
     *
     * This is only available if [ast] is an instance of Right.
     * @throws UnsupportedOperationException when [ast] is Left
     */
    val dependencies: UnitDependencyAnalysis<Exp, Stmt>
}