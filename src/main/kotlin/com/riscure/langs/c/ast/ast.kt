// This is a fully type-annotated C source AST.
// The representation is ported from the CompCert CParser elaborated AST.
package com.riscure.langs.c.ast

import arrow.core.*
import com.riscure.langs.c.index.Index
import com.riscure.langs.c.index.Symbol
import com.riscure.langs.c.index.TUID
import java.nio.file.Path

typealias Name  = String
typealias Ident = String

data class Location(
    val sourceFile: Path,
    /** The line number, with first line being 1 */
    val row: Int,
    /** The column number, with first column being 1 */
    val col: Int
) {
    /**
     * compares the row and col of [this] to [other], ignoring the sourceFile.
     */
    operator fun compareTo(other: Location): Int =
        Pair(row, col).compareTo(Pair(other.row, other.col))
}

data class SourceRange(
    /** begin location of the source range (inclusive) */
    val begin: Location,
    /** end location of the source range (inclusive) */
    val end: Location
) {
    /**
     * Returns true iff [this] range fully encloses [other]
     */
    fun encloses(other: SourceRange): Boolean =
        file == other.file && begin <= other.begin && end >= other.end

    fun encloses(other: Location): Boolean =
        file == other.sourceFile && begin <= other && end >= other

    /** Gets the sourceFile of [begin], which is assumed to match [end]'s */
    val file get() = begin.sourceFile
}

enum class IKind {
      IBoolean
    , IChar, ISChar, IUChar
    , IShort, IUShort
    , IInt, IUInt
    , ILong, IULong
    , ILongLong, IULongLong
}

enum class FKind {
    FFloat, FDouble, FLongDouble
}

/* Attributes */
sealed class AttrArg {
    data class AnIdent(val value: String) : AttrArg()
    data class AnInt(val value: Int) : AttrArg()
    data class AString(val value: String) : AttrArg()
}

sealed class Attr {
    object Constant : Attr()
    object Volatile : Attr()
    object Restrict : Attr()
    data class AlignAs(val alignment: Long) : Attr()
    data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()
}

enum class Storage {
    Default, // used for toplevel names without explicit storage
    Extern,
    Static,
    Auto, // used for block-scoped names without explicit storage
    Register
}

typealias Attrs = List<Attr>

/* Types */
sealed class Type {
    abstract val attrs: Attrs
    abstract fun withAttrs(attrs: Attrs): Type

    fun const() = withAttrs(attrs + Attr.Constant)
    fun restrict() = withAttrs(attrs + Attr.Restrict)

    data class Void (
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Int (
        val kind: IKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Float (
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Ptr (
        val pointeeType: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Array(
        val elementType: Type,
        val size: Option<Long> = None,
        override val attrs: Attrs = listOf()
    ) : Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Fun(
        val returnType: Type,
        val params: List<Param>,
        val vararg: Boolean       = false,
        override val attrs: Attrs = listOf()
    ) : Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Named (
        val id: Ident,
        val underlying: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)

        /** Get the TLID of the referenced typedef */
        val tlid: TLID get() = TLID.typedef(id)
    }
    data class Struct (
        val id: Ident,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
        /** Get the TLID of the referenced struct */
        val tlid: TLID get() = TLID.struct(id)
    }
    data class Union (
        val id: Ident,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)

        /** Get the TLID of the referenced union */
        val tlid: TLID get() = TLID.union(id)
    }
    data class Enum (
        val id: Ident,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)

        /** Get the TLID of the referenced enum */
        val tlid: TLID get() = TLID.enum(id)
    }

    /* _Complex */
    data class Complex(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    /* _Atomic */
    data class Atomic(
        val el: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* A distinguished type for inline compound type declarations */
    data class InlineDeclaration (
        val declaration: Declaration.CompoundTypeDecl,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    companion object {
        @JvmStatic
        val char = Int(IKind.IChar)
        @JvmStatic
        val uint = Int(IKind.IUInt)
        @JvmStatic
        val int = Int(IKind.IInt)
        @JvmStatic
        val ulong = Int(IKind.IULong)

        @JvmStatic
        fun array(el: Type, size: Option<Long>) = Array(el, size)

        @JvmStatic
        fun function(returns: Type, vararg params: Param, variadic: Boolean = false) =
            Fun(returns, params.toList(), variadic)

    }
}

/* Struct or union field */
data class Field(
    val name: String
  , val type: Type
  , val bitfield: Option<Int> = none()
  , val anonymous: Boolean    = false
)

enum class StructOrUnion { Struct, Union }

typealias FieldDecls = List<Field>

data class Param(
    val name: Ident = "",
    val type: Type
)

data class Enumerator(val name: Ident, val key: Long) // TODO missing optional exp?

/**
 * Metadata for the top-level elements.
 */
data class Meta(
    /**
     * The location where this element was parsed.
     */
    val location: Option<SourceRange> = None,
    /**
     * This location reflects {@code #line} directives,
     * as for example outputted by the preprocessor, pointing
     * poking through to the location beneath the {@code #include} directive.
     */
    val presumedLocation: Option<Location> = None,
    val doc: Option<String> = None,
    val fromMain: Boolean = true
) {
    companion object {
        val default = Meta(None, None, None, true)
    }
}

/**
 * The type of declarations, parameterized by the type that represents statements.
 */
sealed interface Declaration<out Exp, out Stmt> {
    val name: Ident

    /**
     * Meta-data comprises the file and location in file for the
     * top-level entity.
     */
    val meta: Meta
    fun withMeta(meta: Meta): Declaration<Exp, Stmt>

    /**
     * Storage for the top-level entity (e.g., default or static).
     */
    val storage: Storage
    fun withStorage(storage: Storage): Declaration<Exp, Stmt>

    val tlid: TLID
    val kind: EntityKind get() = tlid.kind

    /* Mixin */
    sealed interface Typelike { fun definesType(): Type }
    sealed interface CompoundTypeDecl: Declaration<Nothing, Nothing>, Typelike {
        val isAnonymous get() = name.isEmpty()
    }

    data class Var<Exp>(
        override val name: Ident,
        val type: Type,
        val rhs: Option<Exp>          = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta       = Meta.default
    ): Declaration<Exp, Nothing> {
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        val isDefinition: Boolean get() = rhs.isDefined()

        override val tlid: TLID get() =
            if (isDefinition) TLID(name, EntityKind.VarDef)
            else TLID(name, EntityKind.VarDecl)
    }

    data class Fun<out Stmt>(
        override val name: Ident,
        val inline: Boolean,
        val returnType: Type,
        val params: List<Param>,
        val vararg: Boolean           = false,
        val body: Option<Stmt>        = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta       = Meta.default,
    ): Declaration<Nothing, Stmt> {
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        fun type(): Type = Type.Fun(returnType, params, vararg)

        val isDefinition: Boolean get() = body.isDefined()

        override val tlid: TLID get() =
            TLID(name, if (isDefinition) EntityKind.FunDef else EntityKind.FunDecl)
    }

    data class Composite(
        override val name: Ident,
        val structOrUnion: StructOrUnion,
        val fields: FieldDecls,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, CompoundTypeDecl {
        override fun definesType(): Type =
            when (structOrUnion) {
                StructOrUnion.Struct -> Type.Struct(name)
                StructOrUnion.Union  -> Type.Union(name)
            }

        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val tlid: TLID get() =
            TLID(name, when (structOrUnion) {
                StructOrUnion.Union -> EntityKind.Union
                StructOrUnion.Struct -> EntityKind.Struct
            })
    }

    data class Typedef(
        override val name: Ident,
        val underlyingType: Type,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, CompoundTypeDecl {
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override fun definesType(): Type = Type.Named(name, underlyingType)

        override val tlid: TLID get() = TLID(name, EntityKind.Typedef )
    }

    data class EnumDef(
        override val name: Ident,
        val enumerators: List<Enumerator>,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, CompoundTypeDecl {
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override val tlid: TLID get() = TLID(name, EntityKind.Enum)
        override fun definesType(): Type = Type.Enum(name)
    }

    fun ofKind(kind: EntityKind): Boolean = when (kind) {
        EntityKind.FunDecl    -> this is Fun && !isDefinition
        EntityKind.FunDef     -> this is Fun && isDefinition
        EntityKind.Enum       -> this is EnumDef
        EntityKind.Struct     -> this is Composite && structOrUnion == StructOrUnion.Struct
        EntityKind.Union      -> this is Composite && structOrUnion == StructOrUnion.Union
        EntityKind.Typedef    -> this is Typedef
        EntityKind.VarDecl    -> this is Var && !isDefinition
        EntityKind.VarDef     -> this is Var && isDefinition
        EntityKind.Enumerator -> false
    }
}

enum class EntityKind {
    FunDecl,
    FunDef,
    Enum,
    Enumerator,
    Struct,
    Union,
    Typedef,
    VarDef,
    VarDecl;

    val hasDefinition get() = when (this) {
        FunDecl -> false
        VarDecl -> false
        else    -> true
    }

    companion object {
        fun <E,T> kindOf(toplevel: Declaration<E, T>) = toplevel.kind
    }
}

/**
 * Identifies a declaration that is visible at the file-level uniquely
 * within a translation unit.
 */
data class TLID(val name: String, val kind: EntityKind) {
    companion object {
        @JvmStatic fun varDecl(name: String) = TLID(name, EntityKind.VarDecl)
        @JvmStatic fun varDef(name: String) = TLID(name, EntityKind.VarDef)
        @JvmStatic fun function(name: String) = TLID(name, EntityKind.FunDef)
        @JvmStatic fun prototype(name: String) = TLID(name, EntityKind.FunDecl)
        @JvmStatic fun struct(name: String) = TLID(name, EntityKind.Struct)
        @JvmStatic fun union(name: String) = TLID(name, EntityKind.Union)
        @JvmStatic fun enum(name: String) = TLID(name, EntityKind.Enum)
        @JvmStatic fun enumerator(name: String) = TLID(name, EntityKind.Enumerator)
        @JvmStatic fun typedef(name: String) = TLID(name, EntityKind.Typedef)
    }
}

typealias ErasedTranslationUnit = TranslationUnit<*,*>
data class TranslationUnit<out E, out T>(
    val tuid: TUID,

    /** The declarations at the top-level of the file. */
    val decls: List<Declaration<E, T>>
) {
    /**
     * Find a top-level element in this translation unit, based
     * on a given [TLID] [id].
     */
    operator fun get(id: TLID): Option<Declaration<E, T>> = decls
        .find { it.name == id.name && it.ofKind(id.kind) }
        .toOption() // FIXME should also find inline declarations.

    val functions: List<Declaration.Fun<T>> get() = decls.functions()
    val functionDefinitions: List<Declaration.Fun<T>> get() = decls.functions().definitions()
    val structs: List<Declaration.Composite> get() = decls.structs()

    /**
     * Given a function definition identifier, turn it into a declaration only.
     * This will correctly drop the definition if the translation unit already contains a separate
     * declaration of this function. On any other identifier kind this is a noop.
     */
    fun dropDefinition(def: TLID) = when (def.kind) {
        EntityKind.FunDef -> {
            // check if the translation unit has a separate declaration
            when (get(def.copy(kind = EntityKind.FunDecl))) {
                // drop the whole definition
                is Some -> copy(decls = decls.filter { it.tlid == def })
                // turn def into declaration
                is None -> update(def) { (it as Declaration.Fun).copy(body = None) }
            }
        }

        // if not a function definition id, do nothing
        else -> this
    }

    fun dropDefinitions(vararg defs: TLID) =
        defs.fold(this) { acc, it -> acc.dropDefinition(it) }

    /* Map locations to top-level declarations */
    fun getAtLocation(loc: Location): Option<Declaration<E, T>> =
        decls
            .find { decl ->
                when (val range = decl.meta.location) {
                    is Some -> range.value.begin == loc
                    else -> false
                }
            }
            .toOption()

    /* Map locations to top-level declarations */
    fun getEnclosing(range: SourceRange): Option<Declaration<E, T>> =
        decls
            .find { decl ->
                when (val loc = decl.meta.location) {
                    is Some -> loc.value.encloses(range)
                    else -> false
                }
            }
            .toOption()

    /* Map locations to top-level declarations */
    fun getEnclosing(loc: Location): Option<Declaration<E, T>> =
        decls
            .find { decl ->
                when (val range = decl.meta.location) {
                    is Some -> range.value.encloses(loc)
                    else -> false
                }
            }
            .toOption()


    // Indexing a translation unit
    val symbols get() = decls.map { tl -> Symbol(tuid, tl.tlid) }
    val index   get() = Index.create(symbols)

    /**
     * We can use an index as a whitelist to mask a translation unit.
     * This returns a translation unit AST with only those decls that are whitelisted.
     *
     * If the whitelist contains a function prototype, [mask] will do the right thing
     * when it encounters a function definition.
     */
    fun mask(whitelist: Index): TranslationUnit<E, T> {
        fun check(i: Symbol) = whitelist.symbols.contains(i)
        return copy(decls = decls.flatMap { entity ->
            when (entity) {
                // functions are special.
                is Declaration.Fun -> {
                    val funDef = Symbol(tuid, TLID.function(entity.name))
                    val funDec = Symbol(tuid, TLID.prototype(entity.name))

                    when {
                        // definition is whitelisted, also keep declarations
                        check(funDef) -> listOf(entity)
                        // declaration is whitelisted, keep declarations
                        check(funDec) && !entity.isDefinition -> listOf(entity)
                        // declaration is whitelisted, definition is not
                        check(funDec) && entity.isDefinition ->
                            // check if the tu also contains a separate declaration.
                            // if so, just keep that one, else, turn this one into a declaration
                            if (symbols.contains(funDec)) listOf()
                            else listOf(entity.copy(body = None))
                        // otherwise: not whitelisted
                        else -> listOf()
                    }
                }

                // everything else is just directly checked against the whitelist
                else -> listOf(entity).filter { check(Symbol(tuid, entity.tlid)) }
            }
        }
        )
    }
}

fun <E,T> TranslationUnit<E,T>.update(id: TLID, f: (decl: Declaration<E, T>) -> Declaration<E, T>) =
    copy(decls = decls.map { if (it.tlid == id) f(it) else it })

/* filters for toplevel declarations */

fun <E, T> List<Declaration<E, T>>.functions(): List<Declaration.Fun<T>> =
    filterIsInstance<Declaration.Fun<T>>()

fun <E,T> List<Declaration<E, T>>.structs(): List<Declaration.Composite> =
    filterIsInstance<Declaration.Composite>()
        .filter { it.structOrUnion == StructOrUnion.Struct }

fun <T> List<Declaration.Fun<T>>.definitions(): List<Declaration.Fun<T>> =
    filter { it.isDefinition }

fun <T> List<Declaration.Fun<T>>.declarations(): List<Declaration.Fun<T>> =
    filter { !it.isDefinition }

fun <E, T> List<Declaration<E, T>>.typelike(): List<Declaration.Typelike> =
    filterIsInstance<Declaration.Typelike>()