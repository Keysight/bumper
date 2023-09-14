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
    object Weak : Attr()

    @Serializable
    object Packed : Attr()

    @Serializable
    data class AlignAs(val alignment: Long) : Attr()

    @Serializable
    data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()

    companion object {
        /**
         * https://clang.llvm.org/docs/SanitizerCoverage.html#disabling-instrumentation-with-attribute-no-sanitize-coverage
         */
        @JvmStatic
        val noSanitizeCoverage = Attr("no_sanitize", AttrArg.AString("coverage"))

        operator fun invoke(name: String, vararg args: AttrArg) = NamedAttr(name, args.toList())
    }
}

/**
 * We model 'type qualifiers' as attributes
 */
sealed class TypeQualifier : Attr() {
    @Serializable
    object Constant : TypeQualifier()

    @Serializable
    object Volatile : TypeQualifier()

    @Serializable
    object Restrict : TypeQualifier()
}

typealias Attrs = List<Attr>