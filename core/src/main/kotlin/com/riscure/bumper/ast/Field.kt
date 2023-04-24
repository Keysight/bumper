package com.riscure.bumper.ast

import arrow.core.Option
import arrow.core.none

/** Struct or union field */
sealed interface Field {

    val name get() = ""

    data class Named(
        override val name: Ident, /* non-empty! */
        val type: Type,
        val bitfield: Option<Int>
    ): Field {
        constructor(name: Ident, type: Type): this(name, type, none())
    }

    data class Anonymous(
        val structOrUnion: StructOrUnion,
        val subfields: FieldDecls,
        val bitfield: Option<Int> = none()
    ): Field

    companion object {
        /**
         * Utility constructor to construct a named field as Field(..)
         */
        operator fun invoke(id: Ident, type: Type, bitfield: Option<Int> = none()) = Named(id, type, bitfield)
    }
}

typealias FieldDecls  = List<Field>

/**
 * Fold over the named fields in the list, and inside the anonymous structs and unions
 * of anonymous fields.
 */
fun <R> List<Field>.foldFields(initial: R, f: (R, Field.Named) -> R):R =
    this.fold(initial) { acc, field ->
        when (field) {
            is Field.Named     -> f(acc, field)
            is Field.Anonymous -> field.subfields.foldFields(acc, f)
        }
    }

enum class StructOrUnion { Struct, Union }