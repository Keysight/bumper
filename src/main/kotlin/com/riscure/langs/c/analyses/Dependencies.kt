package com.riscure.langs.c.analyses

import arrow.core.*
import com.riscure.langs.c.ast.*

private typealias Result = Either<String, Set<TLID>>

/**
 * Implements a dependency analysis for declarations.
 */
class Dependencies<E, T>(
    val eAnalyzer: (E) -> Result,
    val tAnalyzer: (T) -> Result
) {

    fun getDependencies(decl: Declaration<E,T>): Result = when (decl) {
        is Declaration.Var       ->
            decl.rhs
                .map(eAnalyzer)
                .getOrElse { setOf<TLID>().right() }
                .map { it + decl.type.getDependencies() }
        is Declaration.Composite ->
            decl.fields
                .flatMap { getDependencies(it) }
                .toSet()
                .right()
        is Declaration.EnumDef   ->
            setOf<TLID>().right()
        is Declaration.Fun       ->
            decl.body
                .map(tAnalyzer)
                .getOrElse { setOf<TLID>().right() }
                .map { it + decl.returnType.getDependencies() + decl.params.flatMap { getDependencies(it) }}
        is Declaration.Typedef   ->
            decl.underlyingType.getDependencies().right()
    }

    fun Type.getDependencies(): Set<TLID> = when(this) {
        is Type.Fun    -> returnType.getDependencies() + params.flatMap { it.type.getDependencies() }
        is Type.Array  -> elementType.getDependencies()
        is Type.Ptr    -> pointeeType.getDependencies()
        is Type.Named  -> setOf(tlid)
        is Type.Struct -> setOf(tlid)
        is Type.Union  -> setOf(tlid)
        is Type.Int    -> setOf()
        is Type.Enum   -> setOf()
        is Type.Float  -> setOf()
        is Type.Void   -> setOf()
    }

    fun getDependencies(param: Param): Set<TLID> = TODO()
    fun getDependencies(param: Field): Set<TLID> = TODO()
}