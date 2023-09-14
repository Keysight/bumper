package com.riscure.bumper.ast

/**
 * The type of an enum's value name
 */
data class Enumerator(override val ident: Ident, val key: Long, val enum: TypeRef) : GlobalDeclaration {
   override fun withIdent(id: Ident): Enumerator = copy(ident=id)
}
typealias Enumerators = List<Enumerator>