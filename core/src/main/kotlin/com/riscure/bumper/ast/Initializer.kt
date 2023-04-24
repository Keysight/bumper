package com.riscure.bumper.ast

/** A model of initializers */
sealed interface Initializer {
    data class InitSingle(val exp: Exp): Initializer

    sealed interface Compound: Initializer
    data class InitArray(val elementType: Type, val exps: List<Exp>): Compound
    data class InitStruct(val struct: TypeRef, val initializers: Map<Ident, Initializer>): Compound
    data class InitUnion(
        val union: TypeRef,
        val designator: Ident,
        val initializer: Initializer
    ): Compound
}