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
import kotlin.io.path.extension

/** The type of raw identifiers */
typealias Ident = String

typealias TypeRef  = TLID
typealias Ref = TLID

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

    fun isHeader(): Boolean = sourceFile.extension == "h"

    override fun toString(): String = "$sourceFile ($row:$col)"
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
sealed interface Storage {
    sealed interface Public: Storage

    fun isPublic(): Option<Public> = when (this) {
        is Static -> None
        Auto      -> (this as Public).some()
        Default   -> (this as Public).some()
        Extern    -> (this as Public).some()
        Register  -> (this as Public).some()
    }

    object Static: Storage
    object Default: Public // used for toplevel names without explicit storage
    object Extern: Public
    object Auto: Public // used for block-scoped names without explicit storage
    object Register: Public
}

typealias Attrs = List<Attr>


enum class StructOrUnion { Struct, Union }

sealed interface Type {
    abstract val attrs: Attrs
    abstract fun withAttrs(attrs: Attrs): Type

    /**
     * Fields can have anonymous compound types *that cannot be elaborated*.
     * Those are represented by [Anonymous].
     */
    data class Anonymous(
        val structOrUnion: StructOrUnion,
        val fields: Option<FieldDecls> = None,
        override val attrs: Attrs = listOf()
    ) : Type {
        override fun withAttrs(attrs: Attrs): Anonymous = copy(attrs = attrs)
    }

    /**
     * In our elaborated AST, most types are [Named], either because they are primitive,
     * or declared with a (elaboration generated) name.
     */
    @Serializable
    sealed class Named : Type {
        abstract override fun withAttrs(attrs: Attrs): Named // refine the return type.

        @JvmName("isConst")
        fun const() = withAttrs(attrs + Attr.Constant)

        @JvmName("isRestricted")
        fun restrict() = withAttrs(attrs + Attr.Restrict)
        fun ptr() = Ptr(this)
    }

    sealed interface Defined {
        val ref: TypeRef
    }

    @Serializable
    data class Void(
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    @Serializable
    data class Int(
        val kind: IKind,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    @Serializable
    data class Float(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    @Serializable
    data class Ptr(
        val pointeeType: Named,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    @Serializable
    data class Array(
        val elementType: Named,
        val size: Option<Long> = None,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    @Serializable
    data class Fun(
        val returnType: Named,
        val params: List<Param>,
        val vararg: Boolean = false,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level typedef.
     */
    @Serializable
    data class Typedeffed(override val ref: TypeRef, override val attrs: Attrs = listOf()) : Named(), Defined {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level struct
     */
    @Serializable
    data class Struct(override val ref: TypeRef, override val attrs: Attrs = listOf()) : Named(), Defined {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level union
     */
    @Serializable
    data class Union(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Named(), Defined {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /**
     * Reference to a top-level enum
     */
    @Serializable
    data class Enum(
        override val ref: TypeRef,
        override val attrs: Attrs = listOf()
    ) : Named(), Defined {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /* _Complex */
    @Serializable
    data class Complex(
        val kind: FKind,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /* _Atomic */
    @Serializable
    data class Atomic(
        val elementType: Named,
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    /* The builtin type __builtin_va_list */
    @Serializable
    data class VaList(
        override val attrs: Attrs = listOf()
    ) : Named() {
        override fun withAttrs(attrs: Attrs): Named = copy(attrs = attrs)
    }

    companion object {
        // smart constructors

        @JvmStatic val void = Void()
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
        fun array(el: Named, size: Option<Long>) = Array(el, size)
        @JvmStatic
        fun function(returns: Named, vararg params: Param) = Fun(returns, params.toList(), false)
        @JvmStatic
        fun typedef(ident: String) = Typedeffed(TLID.typedef(ident))
        @JvmStatic
        fun struct(ident: String) = Struct(TLID.struct(ident))
        @JvmStatic
        fun union(ident: String) = Union(TLID.union(ident))

    }
}

/** Struct or union field */
@Serializable
data class Field(
    val name: Ident,
    val type: Type,
    val bitfield: Option<Int> = none()
) {
    val isAnonymous: Boolean get() = name.isEmpty()
}

typealias FieldDecls  = List<Field>

@Serializable
data class Param(val name: Ident, val type: Type.Named) {
    val isAnonymous: Boolean get() = name.isEmpty()

    val ref get() = Exp.Var(name, type)
}
typealias Params = List<Param>

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
        @JvmStatic
        val default = Meta(None, None, None, true)
    }
}

/**
 * Encompassing any declaration that has a global name (so not including e.g. struct declarations in functions)
 */
sealed interface GlobalDeclaration {
    /**
     * Identifier (after elaboration).
     * This is promised to be non-empty to uniquely identify an entity
     * (but an entity can have more than one declaration in the unit).
     */
    val ident: Ident

    fun withIdent(id: Ident): GlobalDeclaration
}

/**
 * The type of an enum's value name
 */
@Serializable
data class Enumerator(override val ident: Ident, val key: Long, val enum: TypeRef) : GlobalDeclaration {
    override fun withIdent(id: Ident): Enumerator = copy(ident=id)
}

typealias Enumerators = List<Enumerator>

/**
 * The type of declarations that can be defined at the top of a translation unit.
 *
 * @param Exp the type that represents expressions
 * @param Stmt the type that represents statements
 */
@Serializable
sealed interface UnitDeclaration<out Exp, out Stmt> : GlobalDeclaration {

    override fun withIdent(id: Ident): UnitDeclaration<Exp, Stmt>

    /**
     * Whether this declaration is also a definition
     */
    val isDefinition: Boolean

    /**
     * Meta-data comprises the file and location in file for the
     * top-level entity.
     */
    val meta: Meta
    fun withMeta(meta: Meta): UnitDeclaration<Exp, Stmt>

    /**
     * Storage for the top-level entity (e.g., default or static).
     */
    val storage: Storage
    fun withStorage(storage: Storage): UnitDeclaration<Exp, Stmt>

    val tlid: TLID get() = TLID(ident, kind)
    val kind: EntityKind
    fun mkSymbol(tuid: TUID): Symbol = Symbol(tuid, tlid)

    /* Mixin */

    /** Everything that is visible to other translation units (non-types) */
    sealed interface Valuelike<out Exp, out Stmt> : UnitDeclaration<Exp, Stmt>

    /** Everything that declares a new type: Struct, Union, Enum */
    sealed interface TypeDeclaration: UnitDeclaration<Nothing, Nothing> {
        val type: Type.Named get() = when (this) {
            is Struct  -> Type.Struct(this.tlid)
            is Union   -> Type.Union(this.tlid)
            is Enum    -> Type.Enum(this.tlid)
            is Typedef -> Type.Typedeffed(this.tlid)
        }
    }

    /** Structs or Unions */
    sealed interface Compound: TypeDeclaration {
        val fields: Option<FieldDecls>

        override val isDefinition get() = fields.isDefined()
    }

    @Serializable
    data class Var<out Exp>(
        override val ident: Ident,
        val type: Type.Named,
        val rhs: Option<Exp> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Valuelike<Exp, Nothing> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition: Boolean get() = rhs.isDefined()
        override val kind: EntityKind get() = EntityKind.Var

        fun toPrototype() = copy(rhs = None)
        fun <E> mapRhs(onExp: (Exp) -> E): Var<E> = Var(ident, type, rhs.map(onExp), storage, meta)
    }

    @Serializable
    data class Fun<out Stmt>(
        override val ident: Ident,
        val inline: Boolean,
        val returnType: Type.Named,
        val params: Params,
        val vararg: Boolean           = false,
        val body: Option<Stmt>        = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default,
    ): Valuelike<Nothing, Stmt> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        val type get(): Type.Named = Type.Fun(returnType, params, vararg)

        override val isDefinition: Boolean get() = body.isDefined()

        override val kind: EntityKind get() = EntityKind.Fun

        fun toPrototype() = copy(body = None)
        fun <S> mapBody(f: (body: Stmt) -> S): Fun<S> =
            Fun(ident, inline, returnType, params, vararg, body.map(f), storage, meta)
    }

    @Serializable
    data class Struct(
        override val ident: Ident,
        override val fields: Option<FieldDecls> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Compound {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override val kind = EntityKind.Struct
    }

    @Serializable
    data class Union(
        override val ident: Ident,
        override val fields: Option<FieldDecls> = none(),
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Compound {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override val isDefinition: Boolean = fields.isDefined()
        override val kind = EntityKind.Union
    }

    @Serializable
    data class Typedef(
        override val ident: Ident,
        val underlyingType: Type.Named,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): UnitDeclaration<Nothing, Nothing>, TypeDeclaration {
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
    ): UnitDeclaration<Nothing, Nothing>, TypeDeclaration {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition = enumerators.isDefined()

        override val kind: EntityKind get() = EntityKind.Enum
    }

    fun ofKind(kind: EntityKind): Boolean = when (kind) {
        EntityKind.Fun     -> this is Fun
        EntityKind.Enum    -> this is Enum
        EntityKind.Struct  -> this is Struct
        EntityKind.Union   -> this is Union
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
        fun <E,T> kindOf(toplevel: UnitDeclaration<E, T>) = toplevel.kind
    }
}

/**
 * Name and kind pair
 */
@Serializable
data class TLID(val name: Ident, val kind: EntityKind) {
    fun symbol(tuid: TUID) = Symbol(tuid, this)

    companion object {
        @JvmStatic fun variable(name: Ident) = TLID(name, EntityKind.Var)
        @JvmStatic fun function(name: Ident) = TLID(name, EntityKind.Fun)
        @JvmStatic fun struct(name: Ident) = TLID(name, EntityKind.Struct)
        @JvmStatic fun union(name: Ident) = TLID(name, EntityKind.Union)
        @JvmStatic fun enum(name: Ident) = TLID(name, EntityKind.Enum)
        @JvmStatic fun typedef(name: Ident) = TLID(name, EntityKind.Typedef)
    }
}

/**
 * @param <E> the type of expressions in the AST.
 * @param <S> the type of statements in the AST.
 */
@Serializable
data class TranslationUnit<out E, out T>(
    /** The (preprocessed) C translation unit identifier */
    val tuid  : TUID,

    /**
     * All declarations in the unit.
     */
    val declarations: List<UnitDeclaration<E, T>>,
) {
    val isEmpty get() = declarations.isEmpty()

    val byIdentifier: Map<TLID, List<UnitDeclaration<E, T>>> by lazy {
        val mapping = mutableMapOf<TLID, MutableList<UnitDeclaration<E,T>>>()
        for (d  in declarations) {
            val ds = mapping.getOrDefault(d.tlid, mutableListOf())
            ds.add(d)
            mapping[d.tlid] = ds
        }

        mapping
    }

    private val nameToEnumerator: Map<Ident, Enumerator> by lazy {
        enums
            .flatMap { enum -> enum.enumerators
                .getOrElse { listOf() }
                .map { enumerator -> Pair(enumerator.ident, enumerator) } }
            .toMap()
    }

    /**
     * Resolves a global [ident] to a global declaration.
     */
    fun resolve(ident: Ident): Option<GlobalDeclaration> = nameToEnumerator[ident]
        .toOption()
        .orElse { declarations.find { it.ident == ident }.toOption() }

    val variables: List<UnitDeclaration.Var<E>>  get() = declarations.variables
    val functions: List<UnitDeclaration.Fun<T>>  get() = declarations.functions
    val structs: List<UnitDeclaration.Struct>    get() = declarations.structs
    val unions: List<UnitDeclaration.Union>      get() = declarations.unions
    val typedefs: List<UnitDeclaration.Typedef>  get() = declarations.typedefs
    val enums: List<UnitDeclaration.Enum>        get() = declarations.enums
    val typeDeclarations  : List<UnitDeclaration.TypeDeclaration> get() = declarations.typeDeclarations
    val valuelikeDeclarations: List<UnitDeclaration.Valuelike<E, T>> get() = declarations.valuelikeDeclarations

    fun filter(predicate: (d: UnitDeclaration<E, T>) -> Boolean) =
        copy(declarations = declarations.filter(predicate))

    /** Map locations to top-level declarations */
    fun getAtLocation(loc: Location): Option<UnitDeclaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.begin == loc }}
            .toOption()

    /** Map locations to top-level declarations */
    fun getEnclosing(range: SourceRange): Option<UnitDeclaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.encloses(range) }}
            .toOption()

    /** Map locations to top-level declarations */
    fun getEnclosing(loc: Location): Option<UnitDeclaration<E, T>> =
        declarations
            .find { decl -> decl.meta.location.exists { it.encloses(loc) }}
            .toOption()

    /**
     * Find declarations in this translation unit with a given [TLID] [id],
     * in the order that they appear.
     */
    fun declarationsForTLID(id: TLID): List<UnitDeclaration<E, T>> = byIdentifier.getOrDefault(id, listOf())

    /**
     * Find declarations in this translation unit with a given [Symbol] [sym],
     * in the order that they appear.
     */
    fun declarationsForSymbol(sym: Symbol): List<UnitDeclaration<E, T>> =
        if (sym.unit == tuid) byIdentifier.getOrDefault(sym.tlid, listOf())
        else listOf()

    fun definitionFor(id: TLID): Option<UnitDeclaration<E,T>> =
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

typealias AnyDeclaration     = UnitDeclaration<Any?, Any?>
typealias AnyTranslationUnit = TranslationUnit<Any?, Any?>

typealias ErasedDeclaration     = UnitDeclaration<Unit, Unit>
typealias ErasedTranslationUnit = TranslationUnit<Unit, Unit>

/**
 * Forget the expressions/statements at the leaves.
 */
fun <E,T> TranslationUnit<E, T>.erase(): ErasedTranslationUnit =
    TranslationUnit(tuid, declarations.map { it.erase() })

/**
 * Forget the expressions/statements on the RHS of definitions.
 */
fun <E,T> UnitDeclaration<E,T>.erase(): ErasedDeclaration = when (this) {
    is UnitDeclaration.TypeDeclaration -> this
    is UnitDeclaration.Fun       -> UnitDeclaration.Fun(
        ident, inline, returnType, params, vararg, body.map { Unit }, storage, meta
    )
    is UnitDeclaration.Var       -> UnitDeclaration.Var(ident, type, rhs.map { Unit }, storage, meta)
}

/**
 * Translation unit is functorial
 */
fun <E1, E2, S1, S2> TranslationUnit<E1, S1>.map(
    onExp : (decl: E1) -> E2,
    onStmt: (stmt: S1) -> S2,
): TranslationUnit<E2, S2> =
    declarations
        .map { d ->
            when (d) {
                is UnitDeclaration.TypeDeclaration -> d
                is UnitDeclaration.Fun       -> d.mapBody(onStmt)
                is UnitDeclaration.Var       -> d.mapRhs(onExp)
            }
        }
        .let { decls -> TranslationUnit(tuid, decls) }

/**
 * Update all declarations with the given TLID.
 */
fun <E,T> TranslationUnit<E,T>.update(id: TLID, f: (decl: UnitDeclaration<E, T>) -> UnitDeclaration<E, T>) =
    copy(declarations = declarations.map { if (it.tlid == id) f(it) else it })

fun <E,T> TranslationUnit<E,T>.collect(transform: (d: UnitDeclaration<E, T>) -> Option<UnitDeclaration<E, T>>) =
    copy(declarations = declarations.flatMap { d -> transform(d).toList() })

// Some convenience extension methods: filters for lists of declarations

val <E, T> List<UnitDeclaration<E, T>>.variables: List<UnitDeclaration.Var<E>> get() =
    filterIsInstance<UnitDeclaration.Var<E>>()

val <E, T> List<UnitDeclaration<E, T>>.functions: List<UnitDeclaration.Fun<T>> get() =
    filterIsInstance<UnitDeclaration.Fun<T>>()

val <E, T> List<UnitDeclaration<E, T>>.typedefs: List<UnitDeclaration.Typedef> get() =
    filterIsInstance<UnitDeclaration.Typedef>()

val <E, T> List<UnitDeclaration<E, T>>.enums: List<UnitDeclaration.Enum> get() =
    filterIsInstance<UnitDeclaration.Enum>()

val <E,T> List<UnitDeclaration<E, T>>.structs: List<UnitDeclaration.Struct> get() =
    filterIsInstance<UnitDeclaration.Struct>()

val <E,T> List<UnitDeclaration<E, T>>.unions: List<UnitDeclaration.Union> get() =
    filterIsInstance<UnitDeclaration.Union>()

val <T> List<UnitDeclaration.Fun<T>>.definitions: List<UnitDeclaration.Fun<T>> get() =
    filter { it.isDefinition }

val <T> List<UnitDeclaration.Fun<T>>.declarations: List<UnitDeclaration.Fun<T>> get() =
    filter { !it.isDefinition }

val <E, T> List<UnitDeclaration<E, T>>.typeDeclarations: List<UnitDeclaration.TypeDeclaration> get() =
    filterIsInstance<UnitDeclaration.TypeDeclaration>()

val <E, T> List<UnitDeclaration<E, T>>.valuelikeDeclarations: List<UnitDeclaration.Valuelike<E, T>> get() =
    filterIsInstance<UnitDeclaration.Valuelike<E,T>>()
