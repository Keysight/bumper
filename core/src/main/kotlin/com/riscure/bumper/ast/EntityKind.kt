package com.riscure.bumper.ast

import kotlinx.serialization.Serializable

@Serializable
enum class EntityKind {
    Fun,
    Enum,
    Struct,
    Union,
    Typedef,
    Var;

    companion object {
        fun <E,T> kindOf(toplevel: UnitDeclaration<E, T>) = toplevel.kind
    }
}