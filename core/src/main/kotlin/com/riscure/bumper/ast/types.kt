package com.riscure.bumper.ast

import arrow.core.*

/** Different integer kinds */
enum class IKind {
    IBoolean
    , IChar, ISChar, IUChar
    , IShort, IUShort
    , IInt, IUInt
    , ILong, IULong
    , ILongLong, IULongLong
}

/** Different float kinds */
enum class FKind {
    FFloat, FDouble, FLongDouble
}

enum class AccessMode {
    ByValue,
    ByCopy,
    ByReference,
    None
}

sealed interface Type {

    /**
     * The types of CompCert's CLight.
     */
    sealed interface Light: Type {
        val accessMode: AccessMode
    }

    val attrs: Attrs
    fun withAttrs(attrs: Attrs): Type // refine type

    // smart constructors
    fun const() = withAttrs(attrs + Attr.Constant)
    fun restrict() = withAttrs(attrs + Attr.Restrict)
    fun ptr() = Ptr(this)

    /**
     * A defined type is one that has a definition in the translation unit.
     */
    sealed interface Defined: Type {
        val ref: TypeRef
    }

    data class Void(
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.None
    }

    data class Int(
        val kind: IKind,
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
    }

    data class Float(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
    }

    data class Ptr(
        val pointeeType: Type,
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
    }

    data class Array(
        val elementType: Type,
        val size: Option<Long> = None,
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByReference
    }

    data class Fun(
        val returnType: Type,
        val params: List<Param>,
        val vararg: Boolean = false,
        override val attrs: Attrs = listOf()
    ) : Light {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByReference
    }

    /**
     * Reference to a top-level typedef.
     */
    data class Typedeffed(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ): Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level struct
     */
    data class Struct(override val ref: TypeRef, override val attrs: Attrs = listOf()) : Light, Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByCopy
    }

    /**
     * Reference to a top-level union
     */
    data class Union(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Light, Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByCopy
    }

    /**
     * Reference to a top-level enum
     */
    data class Enum(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* _Complex */
    data class Complex(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* _Atomic */
    data class Atomic(
        val elementType: Type,
        override val attrs: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* The builtin type __builtin_va_list */
    data class VaList(
        override val attrs: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    companion object {
        // smart constructors

        @JvmStatic val void = Void()
        @JvmStatic val bool = Int(IKind.IBoolean)
        @JvmStatic val char = Int(IKind.IChar)
        @JvmStatic val schar = Int(IKind.ISChar)
        @JvmStatic val uchar = Int(IKind.IUChar)
        @JvmStatic val short = Int(IKind.IShort)
        @JvmStatic val ushort = Int(IKind.IUShort)
        @JvmStatic val uint = Int(IKind.IUInt)
        @JvmStatic val int = Int(IKind.IInt)
        @JvmStatic val long = Int(IKind.ILong)
        @JvmStatic val longlong = Int(IKind.ILongLong)
        @JvmStatic val ulong = Int(IKind.IULong)
        @JvmStatic val ulonglong = Int(IKind.IULongLong)
        @JvmStatic val double = Float(FKind.FDouble)
        @JvmStatic val longdouble = Float(FKind.FLongDouble)
        @JvmStatic val float = Float(FKind.FFloat)

        @JvmStatic
        fun array(el: Type, size: Long) = Array(el, size.some())
        @JvmStatic
        fun array(el: Type) = Array(el, none())
        @JvmStatic
        fun function(returns: Type, vararg params: Param, variadic: Boolean) =
            Fun(returns, params.toList(), variadic)
        @JvmStatic
        fun function(returns: Type, vararg params: Param): Fun = function(returns, *params, variadic = false)
        @JvmStatic
        fun typedef(ident: String) = Typedeffed(TLID.typedef(ident))
        @JvmStatic
        fun struct(ident: String) = Struct(TLID.struct(ident))
        @JvmStatic
        fun union(ident: String) = Union(TLID.union(ident))

    }

    /**
     * Remove the outer layers of typedefs, Atomic, Complex.
     *
     * @returns none if typedef could not be resolved.
     */
    fun unroll(lookup: (tlid: TLID) -> Option<UnitDeclaration.Typedef>): Option<Type> = when (this) {
        is Typedeffed -> lookup(ref).flatMap { it.underlyingType.unroll(lookup) }
        is Atomic -> elementType.some()
        is Complex -> Float(kind).some()
        is Enum -> int.some()
        else -> this.some()
    }

    /**
     * Convert type to an equivalent C Light type.
     *
     * @returns none if typedef could not be resolved, or when [this] is VaList.
     */
    fun toLight(lookup: (tlid: TLID) -> Option<UnitDeclaration.Typedef>): Option<Light> = when(this) {
        is Struct -> this.some()
        is Union -> this.some()
        is Array -> this.some()
        is Float -> this.some()
        is Fun -> this.some()
        is Int -> this.some()
        is Ptr -> this.some()
        is Void -> this.some()
        is Atomic -> elementType.toLight(lookup)
        is Complex -> Float(kind).some()
        is Enum -> int.some()
        is Typedeffed -> lookup(ref).flatMap { it.underlyingType.toLight(lookup) }
        is VaList -> none() // requires platform info
    }
}

