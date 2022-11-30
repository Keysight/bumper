package com.riscure.bumper.index

import com.riscure.bumper.ast.TLID
import kotlinx.serialization.Serializable

@Serializable
data class Symbol(
    val unit: TUID,
    val tlid: TLID
) {
    val name get() = tlid.name
    val kind get() = tlid.kind

    companion object {
        @JvmStatic fun variable(unit: TUID, name: String) =
            Symbol(unit, TLID.variable(name))
        @JvmStatic fun function(unit: TUID, name: String) =
            Symbol(unit, TLID.function(name))
        @JvmStatic fun struct(unit: TUID, name: String) =
            Symbol(unit, TLID.function(name))
        @JvmStatic fun union(unit: TUID, name: String) =
            Symbol(unit, TLID.union(name))
        @JvmStatic fun enum(unit: TUID, name: String) =
            Symbol(unit, TLID.enum(name))
        @JvmStatic fun typedef(unit: TUID, name: String) =
            Symbol(unit, TLID.typedef(name))
    }
}
