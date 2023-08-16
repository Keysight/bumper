package com.riscure.bumper.ast

import arrow.core.Option
import arrow.core.none

/** Struct or union field */
sealed interface Field {

    val name get() = ""

    data class Leaf(
        override val name: Ident, /* can be empty */
        val type: Type,
        val bitfield: Option<Int>
    ): Field {
        constructor(name: Ident, type: Type): this(name, type, none())
    }

    // Anonymous fields with an inline **anonymous** record type are special, because
    // they can be typed by an inline anonymous record.
    // We **cannot** elaborate those inline type definitions to the top-level
    // as we do for named inline record definitions, because that changes the
    // accessibility of the sub-fields and requires renaming of all accesses.
    // Hence, the model distinguishes these fields.
    data class AnonymousRecord(
        val structOrUnion: StructOrUnion,
        val subfields: FieldDecls,
        val bitfield: Option<Int> = none()
    ): Field

    companion object {
        /**
         * Utility constructor to construct a named field as Field(..)
         */
        operator fun invoke(id: Ident, type: Type, bitfield: Option<Int> = none()) = Leaf(id, type, bitfield)
    }
}

typealias FieldDecls  = List<Field>

/**
 * Fold over the named fields in the list, and inside the anonymous structs and unions
 * of anonymous fields.
 */
fun <R> List<Field>.foldFields(initial: R, f: (R, Field.Leaf) -> R):R =
    this.fold(initial) { acc, field ->
        when (field) {
            is Field.Leaf     -> f(acc, field)
            is Field.AnonymousRecord -> field.subfields.foldFields(acc, f)
        }
    }

enum class StructOrUnion { Struct, Union }