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

    private fun Result.merge(that: Result) =
        flatMap { left ->
            that.map { right -> left + right }
        }

    fun Declaration<E,T>.getDependencies(): Result = when (this) {
        is Declaration.Var       ->
            rhs
                .map(eAnalyzer)
                .getOrElse { setOf<TLID>().right() }
                .merge(type.getDependencies())
        is Declaration.Composite ->
            fields
                .map { it.type.getDependencies() }
                .sequence()
                .map { it.flatten().toSet() }
        is Declaration.EnumDef   ->
            setOf<TLID>().right()
        is Declaration.Fun       ->
            body
                .map(tAnalyzer)
                .getOrElse { setOf<TLID>().right() }
                .merge(returnType.getDependencies())
                .merge(params.getDependencies())
        is Declaration.Typedef   ->
            underlyingType.getDependencies()
    }

    fun Type.getDependencies(): Result = when (this) {
        is Type.Fun               ->
            returnType
                .getDependencies()
                .merge(params.getDependencies())
        is Type.Array             -> elementType.getDependencies()
        is Type.Ptr               -> pointeeType.getDependencies()
        is Type.Named             -> setOf(tlid).right()
        is Type.Struct            -> setOf(tlid).right()
        is Type.Union             -> setOf(tlid).right()
        is Type.Int               -> setOf<TLID>().right()
        is Type.Enum              -> setOf<TLID>().right()
        is Type.Float             -> setOf<TLID>().right()
        is Type.Void              -> setOf<TLID>().right()
        is Type.Atomic            -> el.getDependencies()
        is Type.Complex           -> setOf<TLID>().right()
        is Type.InlineDeclaration -> declaration.getDependencies()
    }

    private fun List<Param>.getDependencies(): Result =
        this
            .map { it.type.getDependencies() }
            .sequence()
            .map { it.flatten().toSet() }
}