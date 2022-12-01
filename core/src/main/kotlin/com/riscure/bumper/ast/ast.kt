@file:UseSerializers(OptionSerializer::class)

/**
 * This packages contains a fully type-annotated C source AST *after elaboration*.
 *
 * The representation is similar to CompCert's elaborated AST:
 * - https://github.com/AbsInt/CompCert/blob/master/cparser/C.mli
 * And to Frama-C's elaborated AST:
 * - https://git.frama-c.com/pub/frama-c/-/blob/master/src/kernel_services/ast_data/cil_types.ml
 */
package com.riscure.bumper.ast

import arrow.core.*
import com.riscure.OptionSerializer
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.nio.file.Path

/** The type of raw identifiers */
typealias Ident = String

/** A source location */
@Serializable
data class Location(
    val sourceFile: Path,
    /** The line number, with first line being 1 */
    val row: Int,
    /** The column number, with first column being 1 */
    val col: Int
) {
    /**
     * compares the row and col of this to [other], ignoring the sourceFile.
     */
    operator fun compareTo(other: Location): Int =
        Pair(row, col).compareTo(Pair(other.row, other.col))
}

/** A source range with a begin and end location. */
@Serializable
data class SourceRange(

    /** begin location of the source range (inclusive) */
    val begin: Location,
    /** end location of the source range (inclusive) */
    val end: Location
) {
    /**
     * Returns true iff this range fully encloses [other]
     */
    fun encloses(other: SourceRange): Boolean =
        file == other.file && begin <= other.begin && end >= other.end

    fun encloses(other: Location): Boolean =
        file == other.sourceFile && begin <= other && end >= other

    /** Gets the sourceFile of [begin], which is assumed to match [end]'s */
    val file get() = begin.sourceFile
}

/**
 * This is a comparator for ranges of declarations in the same unit.
 * That means that the ranges that are partially overlapping are considered equal,
 * but nested ones have an order.
 *
 * One source range is less than another if it appears logically before the other.
 * For nested ranges, this means that the innermost is less than the outermost.
 */
object dependencyOrder: Comparator<SourceRange> {
    override fun compare(r1: SourceRange, r2: SourceRange): Int = when {
        r1.end   < r2.begin -> -1                    // r2 disjoint after r1
        r1.begin > r2.end   -> 1                     // r1 disjoint after r2
        r1.begin > r2.begin && r1.end < r2.end -> -1 // r1 nested in r2
        r2.begin > r1.begin && r2.end < r1.end -> 1  // r2 nested in r1
        else                -> 0                     // not disjoint, or exactly overlapping
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

/* Attributes */
@Serializable
sealed class AttrArg {
    @Serializable
    data class AnIdent(val value: Ident) : AttrArg()
    @Serializable
    data class AnInt(val value: Int) : AttrArg()
    @Serializable
    data class AString(val value: String) : AttrArg()
}

@Serializable
sealed class Attr {
    object Constant : Attr()
    object Volatile : Attr()
    object Restrict : Attr()
    @Serializable
    data class AlignAs(val alignment: Long) : Attr()
    @Serializable
    data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()
}

/** Different ways of storing values in C */
enum class Storage {
    Default, // used for toplevel names without explicit storage
    Extern,
    Static,
    Auto, // used for block-scoped names without explicit storage
    Register
}

typealias Attrs = List<Attr>

/**
 * A superset of types that can act as types of a field.
 */
sealed interface FieldType {
    abstract val attrs: Attrs
    abstract fun withAttrs(attrs: Attrs): FieldType

    data class AnonComposite(
        val structOrUnion: StructOrUnion,
        val fields: Option<FieldDecls> = None,
        override val attrs: Attrs = listOf()
    ) : FieldType {
        override fun withAttrs(attrs: Attrs): AnonComposite = copy(attrs = attrs)
    }
}

/* Types */
@Serializable
sealed class Type: FieldType {
    abstract override fun withAttrs(attrs: Attrs): Type // refine the return type.
    fun const() = withAttrs(attrs + Attr.Constant)
    fun restrict() = withAttrs(attrs + Attr.Restrict)
    fun ptr() = Ptr(this)

    @Serializable
    data class Void (
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    @Serializable
    data class Int (
        val kind: IKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    @Serializable
    data class Float (
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    @Serializable
    data class Ptr (
        val pointeeType: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    @Serializable
    data class Array(
        val elementType: Type,
        val size: Option<Long> = None,
        override val attrs: Attrs = listOf()
    ) : Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    @Serializable
    data class Fun(
        val returnType: Type,
        val params: List<Param>,
        val vararg: Boolean       = false,
        override val attrs: Attrs = listOf()
    ) : Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level typedef.
     */
    @Serializable
    data class Typedeffed (
        val ref: Symbol, // elaborated
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level struct
     */
    @Serializable
    data class Struct (
        val ref: Symbol, // elaborated
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level union
     */
    @Serializable
    data class Union (
        val ref: Symbol, // elaborated
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    /**
     * Reference to a top-level enum
     */
    @Serializable
    data class Enum (
        val ref: Symbol, // elaborated
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* _Complex */
    @Serializable
    data class Complex(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* _Atomic */
    @Serializable
    data class Atomic(
        val elementType: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* The builtin type __builtin_va_list */
    @Serializable
    data class VaList(
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    companion object {
        // smart constructors

        @JvmStatic val char = Int(IKind.IChar)
        @JvmStatic val uint = Int(IKind.IUInt)
        @JvmStatic val int = Int(IKind.IInt)
        @JvmStatic val ulong = Int(IKind.IULong)
        @JvmStatic val ulonglong = Int(IKind.IULongLong)
        @JvmStatic val double = Float(FKind.FDouble)
        @JvmStatic val longdouble = Float(FKind.FLongDouble)
        @JvmStatic val float = Float(FKind.FFloat)
        @JvmStatic fun array(el: Type, size: Option<Long>) = Array(el, size)
        @JvmStatic fun function(returns: Type, vararg params: Param, variadic: Boolean = false) =
            Fun(returns, params.toList(), variadic)

    }
}

/** Struct or union field */
@Serializable
data class Field(
    val name: Ident,
    val type: FieldType,
    val bitfield: Option<Int> = none()
) {
    val isAnonymous: Boolean get() = name.isEmpty()
}

enum class StructOrUnion { Struct, Union }

typealias FieldDecls  = List<Field>

@Serializable
data class Param(val name: Ident, val type: Type) {
    val isAnonymous: Boolean get() = name.isEmpty()
}
typealias Params = List<Param>

@Serializable
data class Enumerator(val name: Ident, val key: Long)
typealias Enumerators = List<Enumerator>

/**
 * Metadata for the top-level elements.
 */
@Serializable
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
@Serializable
sealed interface Declaration<out Exp, out Stmt> {
    /**
     * Identifier (after elaboration).
     * This is promised to be non-empty to uniquely identify an entity
     * (but an entity can have more than one declaration in the unit).
     */
    val ident: Ident
    fun withIdent(id: Ident): Declaration<Exp, Stmt>

    /**
     * Whether this declaration is also a definition
     */
    val isDefinition: Boolean

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

    val tlid: TLID get() = TLID(ident, kind)
    val kind: EntityKind
    fun mkSymbol(tuid: TUID): Symbol = Symbol(tuid, tlid)

    /* Mixin */

    /** Everything that can be used as a type */
    sealed interface Typelike

    /** Everything that declares a new type: Struct, Union, Enum */
    sealed interface TypeDeclaration: Declaration<Nothing, Nothing>, Typelike

    @Serializable
    data class Var<out Exp>(
        override val ident: Ident,
        val type: Type,
        val rhs: Option<Exp> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Exp, Nothing> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition: Boolean get() = rhs.isDefined()
        override val kind: EntityKind get() = EntityKind.Var
    }

    @Serializable
    data class Fun<out Stmt>(
        override val ident: Ident,
        val inline: Boolean,
        val returnType: Type,
        val params: Params,
        val vararg: Boolean           = false,
        val body: Option<Stmt>        = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default,
    ): Declaration<Nothing, Stmt> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        fun type(): Type = Type.Fun(returnType, params, vararg)

        override val isDefinition: Boolean get() = body.isDefined()

        override val kind: EntityKind get() = EntityKind.Fun
    }

    /**
     * Struct or Union declaration or definition.
     */
    @Serializable
    data class Composite(
        override val ident: Ident,
        val structOrUnion: StructOrUnion,
        val fields: Option<FieldDecls>      = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, TypeDeclaration {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition: Boolean = fields.isDefined()

        override val kind: EntityKind
            get() =
            when (structOrUnion) {
                StructOrUnion.Union  -> EntityKind.Union
                StructOrUnion.Struct -> EntityKind.Struct
            }
    }

    @Serializable
    data class Typedef(
        override val ident: Ident,
        val underlyingType: Type,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        /** There is no such thing as a typedecl.
         *  A typedef without a type implicitly means typedef int <name>;
         */
        override val isDefinition = true

        override val kind: EntityKind get() = EntityKind.Typedef
    }

    @Serializable
    data class Enum(
        override val ident: Ident,
        val enumerators: Option<Enumerators> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, TypeDeclaration {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition = enumerators.isDefined()

        override val kind: EntityKind get() = EntityKind.Enum
    }

    fun ofKind(kind: EntityKind): Boolean = when (kind) {
        EntityKind.Fun     -> this is Fun
        EntityKind.Enum    -> this is Enum
        EntityKind.Struct  -> this is Composite && structOrUnion == StructOrUnion.Struct
        EntityKind.Union   -> this is Composite && structOrUnion == StructOrUnion.Union
        EntityKind.Typedef -> this is Typedef
        EntityKind.Var     -> this is Var
    }
}

enum class EntityKind {
    Fun,
    Enum,
    Struct,
    Union,
    Typedef,
    Var;

    companion object {
        fun <E,T> kindOf(toplevel: Declaration<E, T>) = toplevel.kind
    }
}

/**
 * Name and kind pair
 */
@Serializable
data class TLID(val name: Ident, val kind: EntityKind) {
    companion object {
        @JvmStatic fun variable(name: Ident) = TLID(name, EntityKind.Var)
        @JvmStatic fun function(name: Ident) = TLID(name, EntityKind.Fun)
        @JvmStatic fun struct(name: Ident) = TLID(name, EntityKind.Struct)
        @JvmStatic fun union(name: Ident) = TLID(name, EntityKind.Union)
        @JvmStatic fun enum(name: Ident) = TLID(name, EntityKind.Enum)
        @JvmStatic fun typedef(name: Ident) = TLID(name, EntityKind.Typedef)
    }
}

@Serializable
data class TranslationUnit<out E, out T>(
    val tuid: TUID,

    /**
     * All declarations in the unit.
     */
    val declarations: List<Declaration<E, T>>,
) {
    val byIdentifier: Map<TLID, List<Declaration<E, T>>> by lazy {
        val mapping = mutableMapOf<TLID, MutableList<Declaration<E,T>>>()
        for (d  in declarations) {
            val ds = mapping.getOrDefault(d.tlid, mutableListOf())
            ds.add(d)
            mapping[d.tlid] = ds
        }

        mapping
    }

    val variables: List<Declaration.Var<E>>  get() = declarations.variables
    val functions: List<Declaration.Fun<T>>  get() = declarations.functions
    val structs: List<Declaration.Composite> get() = declarations.structs
    val unions: List<Declaration.Composite>  get() = declarations.unions
    val typedefs: List<Declaration.Typedef>  get() = declarations.typedefs
    val enums: List<Declaration.Enum>        get() = declarations.enums

    fun filter(predicate: (d: Declaration<E, T>) -> Boolean) =
        copy(declarations = declarations.filter(predicate))

    /** Map locations to top-level declarations */
    fun getAtLocation(loc: Location): Option<Declaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.begin == loc }}
            .toOption()

    /** Map locations to top-level declarations */
    fun getEnclosing(range: SourceRange): Option<Declaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.encloses(range) }}
            .toOption()

    /** Map locations to top-level declarations */
    fun getEnclosing(loc: Location): Option<Declaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.encloses(loc) }}
            .toOption()

    /**
     * Find declarations in this translation unit with a given [TLID] [id],
     * in the order that they appear.
     */
    fun declarationsForTLID(id: TLID): List<Declaration<E, T>> = byIdentifier.getOrDefault(id, listOf())

    /**
     * Find declarations in this translation unit with a given [Symbol] [sym],
     * in the order that they appear.
     */
    fun declarationsForSymbol(sym: Symbol): List<Declaration<E, T>> =
        if (sym.unit == tuid) byIdentifier.getOrDefault(sym.tlid, listOf())
        else listOf()

    fun definitionFor(id: TLID): Option<Declaration<E,T>> =
        byIdentifier[id]
            ?.find { it.isDefinition }
            .toOption()

    /**
     * Given a function definition identifier, turn it into a declaration only.
     * This will correctly drop the definition if the translation unit already contains a separate
     * declaration of this function. On any other identifier kind this is a noop.
     */
    /*
    fun dropDefinition(def: TLID) = when (def.kind) {
        def.kind == EntityKind.Fun -> {
            // check if the translation unit has a separate declaration
            when (get(def.copy(kind = EntityKind.FunDecl))) {
                // drop the whole definition
                is Some -> copy(decls = decls.filter { d -> d.tlid.all { it != def } })
                // turn def into declaration
                is None -> update(def) { (it as Declaration.Fun).copy(body = None) }
            }
        }

        // if not a function definition id, do nothing
        else -> this
    }

    fun dropDefinitions(vararg defs: TLID) =
        defs.fold(this) { acc, it -> acc.dropDefinition(it) }
    */


    /*
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

                else ->
                    // everything else is just directly checked against the whitelist
                    if (entity.tlid.exists { id -> check(Symbol(tuid, id)) })
                        listOf(entity)
                    else listOf()
            }
        }
        )
    }
    */
}

typealias AnyDeclaration     = Declaration<Any?, Any?>
typealias AnyTranslationUnit = TranslationUnit<Any?, Any?>

typealias ErasedDeclaration     = Declaration<Unit, Unit>
typealias ErasedTranslationUnit = TranslationUnit<Unit, Unit>

/**
 * Forget the expressions/statements at the leaves.
 */
fun <E,T> TranslationUnit<E, T>.erase(): ErasedTranslationUnit =
    TranslationUnit(tuid, declarations.map { it.erase() })

/**
 * Forget the expressions/statements on the RHS of definitions.
 */
fun <E,T> Declaration<E,T>.erase(): ErasedDeclaration = when (this) {
    is Declaration.Composite -> this
    is Declaration.Enum      -> this
    is Declaration.Typedef   -> this
    is Declaration.Fun       -> Declaration.Fun(
        ident, inline, returnType, params, vararg, body.map { Unit }, storage, meta
    )
    is Declaration.Var       -> Declaration.Var(ident, type, rhs.map { Unit }, storage, meta)
}

/**
 * Update all declarations with the given TLID.
 */
fun <E,T> TranslationUnit<E, T>.update(id: TLID, f: (decl: Declaration<E, T>) -> Declaration<E, T>) =
    copy(declarations = declarations.map { if (it.tlid == id) f(it) else it })

// Some convenience extension methods: filters for lists of declarations

val <E, T> List<Declaration<E, T>>.variables: List<Declaration.Var<E>> get() =
    filterIsInstance<Declaration.Var<E>>()

val <E, T> List<Declaration<E, T>>.functions: List<Declaration.Fun<T>> get() =
    filterIsInstance<Declaration.Fun<T>>()

val <E, T> List<Declaration<E, T>>.typedefs: List<Declaration.Typedef> get() =
    filterIsInstance<Declaration.Typedef>()

val <E, T> List<Declaration<E, T>>.enums: List<Declaration.Enum> get() =
    filterIsInstance<Declaration.Enum>()

val <E,T> List<Declaration<E, T>>.structs: List<Declaration.Composite> get() =
    filterIsInstance<Declaration.Composite>()
        .filter { it.structOrUnion == StructOrUnion.Struct }

val <E,T> List<Declaration<E, T>>.unions: List<Declaration.Composite> get() =
    filterIsInstance<Declaration.Composite>()
        .filter { it.structOrUnion == StructOrUnion.Union }

val <T> List<Declaration.Fun<T>>.definitions: List<Declaration.Fun<T>> get() =
    filter { it.isDefinition }

val <T> List<Declaration.Fun<T>>.declarations: List<Declaration.Fun<T>> get() =
    filter { !it.isDefinition }

val <E, T> List<Declaration<E, T>>.typelike: List<Declaration.Typelike> get() =
    filterIsInstance<Declaration.Typelike>()