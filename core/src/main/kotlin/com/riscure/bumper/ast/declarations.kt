package com.riscure.bumper.ast

import kotlinx.serialization.Serializable

@Serializable
data class Param(val name: Ident, val type: Type) {
    val isAnonymous: Boolean get() = name.isEmpty()

    val ref get() = Exp.Var(name, type)
}

typealias Params = List<Param>