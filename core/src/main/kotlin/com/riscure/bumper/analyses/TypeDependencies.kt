package com.riscure.bumper.analyses

import com.riscure.bumper.ast.*

fun typeDependencies(type: Type) = type.fold(setOf<TLID>()) { acc, type ->
    acc + when (type) {
        is Type.Struct     -> setOf(type.ref)
        is Type.Union      -> setOf(type.ref)
        is Type.Enum       -> setOf(type.ref)
        is Type.Typedeffed -> setOf(type.ref)
        else -> setOf()
    }
}

fun typeDependencies(types: Collection<Type>) =
    types.fold(setOf<TLID>()) { acc, t -> acc + typeDependencies(t) }