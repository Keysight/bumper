package com.riscure.langs.c.index

import com.riscure.langs.c.ast.Site
import com.riscure.langs.c.ast.TLID

/* Uniquely identify a symbol across translation units */
data class Symbol(
    val unit: TUID,
    val tlid: TLID,
    val site: Site
) {
    val name get() = tlid.name
    val kind get() = tlid.kind

    companion object {
        @JvmStatic fun variable(unit: TUID, name: String, site: Site) =
            Symbol(unit, TLID.variable(name), site)
        @JvmStatic fun function(unit: TUID, name: String, site: Site) =
            Symbol(unit, TLID.function(name), site)
        @JvmStatic fun struct(unit: TUID, name: String, site: Site) =
            Symbol(unit, TLID.function(name), site)
        @JvmStatic fun union(unit: TUID, name: String, site: Site) =
            Symbol(unit, TLID.union(name), site)
        @JvmStatic fun enum(unit: TUID, name: String, site: Site) =
            Symbol(unit, TLID.enum(name), site)
    }
}