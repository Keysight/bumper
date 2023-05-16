package com.riscure.bumper.ast

import arrow.core.*
import com.riscure.bumper.serialization.OptionAsNullable
import kotlinx.serialization.Serializable

/** All the different integer kinds of C */
@Serializable enum class IKind {
    IBoolean
    , IChar, ISChar, IUChar
    , IShort, IUShort
    , IInt, IUInt
    , ILong, IULong
    , ILongLong, IULongLong
}

/** All the different float kinds of C */
@Serializable enum class FKind {
    FFloat, FDouble, FLongDouble
}

/**
 * Some types are accessed by value, other by value, yet others by copying.
 *
 * @see Type.Core.accessMode
 */
@Serializable enum class AccessMode {
    ByValue,
    ByCopy,
    ByReference,
    None
}

/**
 * A model of C types.
 */
@Serializable sealed interface Type {

    /**
     * The core C types.
     *
     * All properties of C types are defined on the core types.
     * Typedeffed types and opaque implementation defined types get their properties
     * from their eventual expansion to core types.
     */
    sealed interface Core: Type {
        override fun modifyAttrs(f: (Attrs) -> Attrs): Core = withAttrs(f(attrsOnType))

        override fun withAttrs(newAttrs: Attrs): Core =  when (this) {
            is Enum -> copy(attrsOnType = newAttrs)
            is Struct -> copy(attrsOnType = newAttrs)
            is Union -> copy(attrsOnType = newAttrs)
            is Array -> copy(attrsOnType = newAttrs)
            is Fun -> copy(attrsOnType = newAttrs)
            is Float -> copy(attrsOnType = newAttrs)
            is Int -> copy(attrsOnType = newAttrs)
            is Ptr -> copy(attrsOnType = newAttrs)
            is Void -> copy(attrsOnType = newAttrs)
        }

        val accessMode: AccessMode get() = when (this) {
            is Enum -> AccessMode.ByValue
            is Float -> AccessMode.ByValue
            is Int -> AccessMode.ByValue
            is Ptr -> AccessMode.ByValue

            is Array -> AccessMode.ByReference
            is Fun -> AccessMode.ByReference

            is Struct -> AccessMode.ByCopy
            is Union -> AccessMode.ByCopy

            is Void -> AccessMode.None
        }

        /**
         * A type is complete w.r.t. a given type environment iff its size can be computed.
         */
        override fun isComplete(typeEnv: TypeEnv): Boolean = when (this) {
            is Array ->
                // C treats arrays as complete even when the array length is unknown, I think.
                // Because it assumes them to be length 0 when computing sizes.
                elementType.isComplete(typeEnv)
            is Record ->
                typeEnv
                    .fields(ref)
                    .map { fields ->
                        // recursively check completeness of the field types
                        fields.foldFields(true) { acc, fs -> acc && fs.type.isComplete(typeEnv) }
                    }
                    .getOrElse { false }
            is Fun -> true // decays to point
            is Enum -> true // represented by int
            is Float -> true
            is Int -> true
            is Ptr -> true
            is Void -> false // always incomplete!
        }

        /**
         * Whether this type can be the returned from a C function.
         */
        override fun isReturnable(typeEnv: TypeEnv): Boolean =
            isComplete(typeEnv)
                    && this !is Array
                    && this !is Fun

        override fun normalize(typeEnv: TypeEnv): TypeLookup<Core>

        /**
         * Some types decay to pointers if necessary.
         */
        fun decay(): Core = when (this) {
            is Array -> this.elementType.ptr()
            is Fun   -> this.ptr()
            else     -> this
        }

        /**
         * Whether the type is considered constant.
         */
        override fun isConstant(typeEnv: TypeEnv) =
            attributes(typeEnv).contains(Attr.Constant) || when (this) {
                is Record -> typeEnv
                    .fields(ref)
                    .map { it.foldFields(false) { acc, field -> acc || field.type.isConstant(typeEnv)} }
                    .getOrElse { false }

                else      -> false
            }

        /* Whether an expression of this type can be modifiable. See:
         * https://learn.microsoft.com/en-us/cpp/c-language/l-value-and-r-value-expressions?view=msvc-170
         */
        override fun isModifiable(typeEnv: TypeEnv) = isComplete(typeEnv) && !isConstant(typeEnv)

        /**
         * Attributes of the type including those attached to their definitions if applicable.
         */
        override fun attributes(typeEnv: TypeEnv) = attrsOnType + when (this) {
            is Array -> elementType.attributes(typeEnv)
            is Defined -> typeEnv
                .lookup(ref)
                .map { it.type.attrsOnType }
                .getOrElse { listOf() }
            else       -> listOf()
        }
    }

    /** Some core types are called scalar **/
    sealed interface Scalar: Core
    /** Some core types are called aggregate **/
    sealed interface Aggregate: Core

    /** A defined type is one that has a definition in the translation unit */
    sealed interface Defined: Type {
        val ref: TypeRef
    }

    /** Record types are aggregate defined types */
    sealed interface Record: Aggregate, Defined

    /**
     * Remove the outer layers of typedefs, Atomic, Complex to get to the normalized representation type,
     * while propagating attributes.
     *
     * @returns none if typedef could not be resolved.
     */
    fun toCore(env: TypeEnv): TypeLookup<Core> = when (this) {
        is Core -> this.right()
        is Typedeffed -> env
            .typedefs(ref)
            .flatMap { it.underlyingType.toCore(env) }
            .map { it.modifyAttrs { it.plus(attrsOnType) } }
        is VaList -> env.builtins.__builtin_va_list.underlyingType
            .toCore(env)
            .map { it.modifyAttrs { it.plus(attrsOnType) } }
    }

    /**
     * Fully normalize the type to the subset [Core].
     *
     * In contrast to [toCore] this also normalizes children of this type,
     * so you can expect that, for example, the pointee or the element type of an [Array] are
     * [Core] instances.
     */
    fun normalize(typeEnv: TypeEnv): TypeLookup<Core> = toCore(typeEnv)
        .flatMap { it.normalize(typeEnv) }

    /**
     * @see Core.isComplete
     */
    fun isComplete(typeEnv: TypeEnv): Boolean = toCore(typeEnv)
        .map { it.isComplete(typeEnv) }
        .getOrElse { false }

    /**
     * @see Core.isModifiable
     */
    fun isModifiable(typeEnv: TypeEnv): Boolean = toCore(typeEnv)
        .map { it.isModifiable(typeEnv) }
        .getOrElse { false }

    /**
     * @see Core.isConstant
     */
    fun isConstant(typeEnv: TypeEnv): Boolean = toCore(typeEnv)
        .map { it.isConstant(typeEnv) }
        .getOrElse { false }

    /**
     * @see Core.isReturnable
     */
    fun isReturnable(typeEnv: TypeEnv): Boolean = toCore(typeEnv)
        .map { it.isReturnable(typeEnv) }
        .getOrElse { false }

    /**
     * @see Core.attributes
     */
    fun attributes(typeEnv: TypeEnv): Attrs = toCore(typeEnv)
        .map { it.attributes(typeEnv) }
        .getOrElse { listOf() }

    /**
     * The attributes on this type.
     * When inspecting the attributes, you should usually consult [attributes] instead.
     */
    val attrsOnType: Attrs
    fun withAttrs(newAttrs: Attrs): Type = when (this) {
        is Typedeffed -> copy(attrsOnType = newAttrs)
        is VaList     -> copy(attrsOnType = newAttrs)
        is Core       -> withAttrs(newAttrs)
    }
    fun modifyAttrs(f: (Attrs) -> Attrs): Type = withAttrs(f(attrsOnType))

    // smart constructors
    fun const() = modifyAttrs {it + Attr.Constant }
    fun restrict() = modifyAttrs { it + Attr.Restrict }
    fun ptr() = Ptr(this)

    @Serializable
    data class Void(
        override val attrsOnType: Attrs = listOf()
    ) : Core {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Void> = this.right()
    }

    @Serializable
    data class Int(
        val kind: IKind,
        override val attrsOnType: Attrs = listOf()
    ) : Scalar {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Int> = this.right()
    }

    @Serializable
    data class Float(
        val kind: FKind,
        override val attrsOnType: Attrs = listOf()
    ) : Scalar {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Float> = this.right()
    }

    @Serializable
    data class Ptr(
        val pointeeType: Type,
        override val attrsOnType: Attrs = listOf()
    ) : Scalar {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Ptr> = this.right()
    }

    @Serializable
    data class Array(
        val elementType: Type,
        val size: OptionAsNullable<Long> = None,
        override val attrsOnType: Attrs = listOf()
    ) : Aggregate {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Array> = this.right()
    }

    @Serializable
    data class Fun(
        val returnType: Type,
        val params: Params = listOf(),
        val vararg: Boolean = false,
        override val attrsOnType: Attrs = listOf()
    ) : Core {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Fun> =
            params
                .map { it.normalizeType(typeEnv) }
                .sequence()
                .flatMap { ps ->
                    returnType
                        .normalize(typeEnv)
                        .map { retty -> Fun(retty, ps) }
                }
    }

    /**
     * Reference to a top-level typedef.
     */
    @Serializable
    data class Typedeffed(
        override val ref: TypeRef,
        override val attrsOnType: Attrs = listOf()
    ): Defined

    /**
     * Reference to a top-level struct
     */
    @Serializable
    data class Struct(override val ref: TypeRef, override val attrsOnType: Attrs = listOf()) :
        Record {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Struct> = this.right()
    }

    /**
     * Reference to a top-level union
     */
    @Serializable
    data class Union(
        override val ref: TypeRef,
        override val attrsOnType: Attrs = listOf()
    ) : Record {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Union> = this.right()
    }

    /**
     * Reference to a top-level enum
     */
    @Serializable
    data class Enum(
        override val ref: TypeRef,
        override val attrsOnType: Attrs = listOf()
    ) : Scalar, Defined {
        override fun normalize(typeEnv: TypeEnv): TypeLookup<Enum> = this.right()
    }


    /**
     * The type __builtin_va_list is an opaque builtin type definition.
     *
     * We represent it in our model, because otherwise it would be determined already by the parser,
     * and we lose portability of modeled C code.
     */
    @Serializable
    data class VaList(
        override val attrsOnType: Attrs = listOf()
    ) : Type

    /* _Complex */
    /*data class Complex(
        val kind: FKind,
        override val attrsOnType: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }*/

    /* _Atomic */
    /*data class Atomic(
        val elementType: Type,
        override val attrsOnType: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }*/

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
}
