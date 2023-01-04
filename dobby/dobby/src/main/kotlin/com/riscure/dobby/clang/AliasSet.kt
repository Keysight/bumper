package com.riscure.dobby.clang

/**
 * Simple union-find variant for option aliasing.
 */
class AliasSet {
    val opt: OptionSpec
    private var aliasOf: AliasSet

    constructor(opt: OptionSpec): super() {
        this.opt = opt

        // tie the knot.
        // This reflects that we want to construct a *reflexive* transitive closure.
        this.aliasOf = this
    }

    fun alias(canonical: AliasSet) = apply { aliasOf = canonical }

    fun representative(): AliasSet {
        val repr = if (this == aliasOf) this else aliasOf.representative()

        // Flexing some path compression bro.
        this.aliasOf = repr

        return repr
    }

}