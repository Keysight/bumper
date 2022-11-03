package com.riscure.langs.c.index

import com.riscure.langs.c.ast.TLID

/* Uniquely identify a symbol across translation units */
data class Symbol(
    val unit: TUID,
    val entity: TLID
) {
    val name get() = entity.name
    val kind get() = entity.kind

    companion object {
        @JvmStatic fun variable(unit: TUID, name: String) = Symbol(unit, TLID.variable(name))
        @JvmStatic fun function(unit: TUID, name: String) = Symbol(unit, TLID.function(name))
        @JvmStatic fun struct(unit: TUID, name: String) = Symbol(unit, TLID.function(name))
        @JvmStatic fun union(unit: TUID, name: String) = Symbol(unit, TLID.union(name))
        @JvmStatic fun enum(unit: TUID, name: String) = Symbol(unit, TLID.enum(name))
    }
}