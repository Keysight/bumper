package com.riscure.bumper.ast

import kotlinx.serialization.Serializable

/** Attribute arguments */
@Serializable
sealed class AttrArg {
    @Serializable
    data class AnIdent(val value: Ident) : AttrArg()
    @Serializable
    data class AnInt(val value: Int) : AttrArg()
    @Serializable
    data class AString(val value: String) : AttrArg()
}

/** Type attributes */
@Serializable
sealed class Attr {
    @Serializable
    object Constant : Attr()
    @Serializable
    object Volatile : Attr()
    @Serializable
    object Restrict : Attr()
    @Serializable
    data class AlignAs(val alignment: Long) : Attr()
    // @Serializable
    // data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()
}
typealias Attrs = List<Attr>