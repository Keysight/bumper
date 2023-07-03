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

    val isValue get() = this == Fun || this == Var
    val isType get() = this == Enum || this == Struct || this == Union || this == Typedef

    companion object {
        fun <E,T> kindOf(toplevel: UnitDeclaration<E, T>) = toplevel.kind
    }
}