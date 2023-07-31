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
    // the following are usually called 'type qualifiers'
    // but we model them as attributes.
    @Serializable
    object Constant : Attr()
    @Serializable
    object Volatile : Attr()
    @Serializable
    object Restrict : Attr()


    @Serializable
    object Weak: Attr()
    @Serializable
    data class AlignAs(val alignment: Long) : Attr()

    @Serializable
    data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()

    companion object {
        /**
         * https://clang.llvm.org/docs/SanitizerCoverage.html#disabling-instrumentation-with-attribute-no-sanitize-coverage
         */
        @JvmStatic val noSanitizeCoverage = Attr("no_sanitize", AttrArg.AString("coverage"))

        operator fun invoke(name: String, vararg args: AttrArg) = NamedAttr(name, args.toList())
    }
}

typealias Attrs = List<Attr>