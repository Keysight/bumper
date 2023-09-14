package com.riscure.bumper.ast

import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import kotlinx.serialization.Serializable

/**
 * Name and kind pair for 'top level' identifiers in C translation units.
 */
@Serializable
data class TLID(val name: Ident, val kind: EntityKind) {
    fun symbol(tuid: TUID) = Symbol(tuid, this)

    /** Print the TLID as one would refer to it in a C program */
    val pretty get() = when (kind) {
        EntityKind.Enum    -> "enum $name"
        EntityKind.Struct  -> "struct $name"
        EntityKind.Union   -> "union $name"
        else               -> name
    }

    companion object {
        @JvmStatic fun variable(name: Ident) = TLID(name, EntityKind.Var)
        @JvmStatic fun function(name: Ident) = TLID(name, EntityKind.Fun)
        @JvmStatic fun struct(name: Ident) = TLID(name, EntityKind.Struct)
        @JvmStatic fun union(name: Ident) = TLID(name, EntityKind.Union)
        @JvmStatic fun enum(name: Ident) = TLID(name, EntityKind.Enum)
        @JvmStatic fun typedef(name: Ident) = TLID(name, EntityKind.Typedef)
    }
}