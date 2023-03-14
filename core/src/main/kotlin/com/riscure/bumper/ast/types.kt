package com.riscure.bumper.ast

import arrow.core.*

private typealias TypeLookup<T> = Either<TypeEnv.Missing, T>
interface TypeEnv {
    data class Missing(val type: TLID): Exception() {
        override val message: String?
            get() = "Failed to lookup definition of ${type.kind.toString().lowercase()} ${type.name}"
    }

    val builtins: Builtins

    fun lookup(tlid: TLID): TypeLookup<UnitDeclaration.TypeDeclaration>

    fun typedefs(tlid: TLID): TypeLookup<UnitDeclaration.Typedef> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Typedef) it.right()
                else Missing(tlid).left()
            }
    fun structs(tlid: TLID): TypeLookup<UnitDeclaration.Struct> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Struct) it.right()
                else Missing(tlid).left()
            }
    fun unions(tlid: TLID): TypeLookup<UnitDeclaration.Union> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Union) it.right()
                else Missing(tlid).left()
            }
    fun enums(tlid: TLID): TypeLookup<UnitDeclaration.Enum> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Enum) it.right()
                else Missing(tlid).left()
            }
    fun fields(tlid: TLID): TypeLookup<FieldDecls> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Compound) it.fields.toEither { Missing(tlid) }
                else Missing(tlid).left()
            }
}

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

    sealed interface Unrolled: Type {
        val accessMode: AccessMode

        /**
         * A type is complete w.r.t. a given type environment when its size
         * can be computed.
         */
        override fun isComplete(typeEnv: TypeEnv): Boolean
    }

    fun isComplete(typeEnv: TypeEnv): Boolean = unroll(typeEnv)
        .map { it.isComplete(typeEnv) }
        .getOrElse { false }

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
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.None
        override fun isComplete(typeEnv: TypeEnv): Boolean = true
    }

    data class Int(
        val kind: IKind,
        override val attrs: Attrs = listOf()
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
        override fun isComplete(typeEnv: TypeEnv): Boolean = true
    }

    data class Float(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
        override fun isComplete(typeEnv: TypeEnv): Boolean = true
    }

    data class Ptr(
        val pointeeType: Type,
        override val attrs: Attrs = listOf()
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByValue
        override fun isComplete(typeEnv: TypeEnv): Boolean = true
    }

    data class Array(
        val elementType: Type,
        val size: Option<Long> = None,
        override val attrs: Attrs = listOf()
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByReference
        override fun isComplete(typeEnv: TypeEnv): Boolean =
            // C treats arrays as complete even when the array size is variable.
            elementType.isComplete(typeEnv)
    }

    data class Fun(
        val returnType: Type,
        val params: List<Param>,
        val vararg: Boolean = false,
        override val attrs: Attrs = listOf()
    ) : Unrolled {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByReference
        override fun isComplete(typeEnv: TypeEnv): Boolean = true // functions decay to pointers
    }

    /**
     * Reference to a top-level typedef.
     */
    data class Typedeffed(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ): Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override fun isComplete(typeEnv: TypeEnv): Boolean =
            typeEnv
                .typedefs(ref)
                .map { it.underlyingType.isComplete(typeEnv) }
                .getOrElse { false }
    }

    /**
     * Reference to a top-level struct
     */
    data class Struct(override val ref: TypeRef, override val attrs: Attrs = listOf()) : Unrolled, Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByCopy
        override fun isComplete(typeEnv: TypeEnv): Boolean =
            typeEnv
                .structs(ref)
                .map { struct ->
                    // recursively check completeness of the field types
                    struct.foldFields(true) { acc, fs -> acc && fs.type.isComplete(typeEnv) }
                }
                .getOrElse { false }
    }

    /**
     * Reference to a top-level union
     */
    data class Union(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Unrolled, Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override val accessMode get() = AccessMode.ByCopy
        override fun isComplete(typeEnv: TypeEnv): Boolean =
            typeEnv
                .unions(ref)
                .map { struct ->
                    // recursively check completeness of the field types
                    struct.foldFields(true) { acc, fs -> acc && fs.type.isComplete(typeEnv) }
                }
                .getOrElse { false }
    }

    /**
     * Reference to a top-level enum
     */
    data class Enum(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Defined {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        override fun isComplete(typeEnv: TypeEnv): Boolean = true
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

    /* The type __builtin_va_list is an opaque builtin type definition */
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
     * Remove the outer layers of typedefs, Atomic, Complex to get to the normalized representation type.
     *
     * @returns none if typedef could not be resolved.
     */
    fun unroll(env: TypeEnv): TypeLookup<Type.Unrolled> = when (this) {
        is Typedeffed -> env.typedefs(ref).flatMap { it.underlyingType.unroll(env) }
        is Atomic -> elementType.unroll(env)
        is Complex -> Float(kind).right()
        is Enum -> int.right()
        is Struct -> this.right()
        is Union -> this.right()
        is Array -> this.right()
        is Float -> this.right()
        is Fun -> this.right()
        is Int -> this.right()
        is Ptr -> this.right()
        is Void -> this.right()
        is VaList -> env.builtins.__builtin_va_list.underlyingType.unroll(env)
    }
}

