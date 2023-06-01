package com.riscure.bumper.analyses

import com.riscure.bumper.ast.*

private fun gatherDeps(acc: Set<TLID>, type: Type): Set<TLID> =
    acc + when (type) {
        is Type.Struct     -> setOf(type.ref)
        is Type.Union      -> setOf(type.ref)
        is Type.Enum       -> setOf(type.ref)
        is Type.Typedeffed -> setOf(type.ref)
        else -> setOf()
    }

/**
 * Extract the set of *direct* type identifiers that [type] depends on.
 */
fun typeDependencies(type: Type): Set<TLID> = type.bottomUp(setOf(), ::gatherDeps)

/**
 * Extract the set of *direct* type identifiers that [types] depends on.
 */
fun typeDependencies(types: Collection<Type>): Set<TLID> =
    types.fold(setOf()) { acc, t -> acc + typeDependencies(t) }

/**
 * Extract the set of *direct* type identifiers that [type] depends on.
 */
fun typeDependencies(type: UnitDeclaration.TypeDeclaration) =
    type.bottomUp(setOf(), ::gatherDeps)