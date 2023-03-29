/**
 * This package contains a fully type-annotated C source AST *after elaboration*.
 *
 * The representation is similar to CompCert's elaborated AST:
 * - https://github.com/AbsInt/CompCert/blob/master/cparser/C.mli
 * And to Frama-C's elaborated AST:
 * - https://git.frama-c.com/pub/frama-c/-/blob/master/src/kernel_services/ast_data/cil_types.ml
 */
package com.riscure.bumper.ast

import arrow.core.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID

data class Prototype(val tlid: TLID, val type: Type)

/* Attributes */
sealed class AttrArg {
    data class AnIdent(val value: Ident) : AttrArg()
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

enum class StructOrUnion { Struct, Union }

/** Struct or union field */
sealed interface Field {

    val name get() = ""

    data class Named(
        override val name: Ident, /* non-empty! */
        val type: Type,
        val bitfield: Option<Int>
    ): Field {
        constructor(name: Ident, type: Type): this(name, type, none())
    }

    data class Anonymous(
        val structOrUnion: StructOrUnion,
        val subfields: FieldDecls,
        val bitfield: Option<Int> = none()
    ): Field

    companion object {
        /**
         * Utility constructor to construct a named field as Field(..)
         */
        operator fun invoke(id: Ident, type: Type, bitfield: Option<Int> = none()) = Named(id, type, bitfield)
    }
}
typealias FieldDecls  = List<Field>

/**
 * Fold over the named fields in the list, and inside the anonymous structs and unions
 * of anonymous fields.
 */
fun <R> List<Field>.foldFields(initial: R, f: (R, Field.Named) -> R):R =
    this.fold(initial) { acc, field ->
        when (field) {
            is Field.Named     -> f(acc, field)
            is Field.Anonymous -> field.subfields.foldFields(acc, f)
        }
    }

data class Param(val name: Ident, val type: Type) {
    val isAnonymous: Boolean get() = name.isEmpty()

    val ref get() = Exp.Var(name, type)
}
typealias Params = List<Param>

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
    val presumedHeader get() = presumedLocation.filter { it.isHeader() }

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
data class Enumerator(override val ident: Ident, val key: Long, val enum: TypeRef) : GlobalDeclaration {
   override fun withIdent(id: Ident): Enumerator = copy(ident=id)
}

typealias Enumerators = List<Enumerator>

/**
 * The type of declarations that can be defined at the top of a translation unit.
 *
 * @param E the type that represents expressions
 * @param S the type that represents statements
 */
sealed interface UnitDeclaration<out E, out S> : GlobalDeclaration {

    override fun withIdent(id: Ident): UnitDeclaration<E, S>

    /**
     * Whether this declaration is also a definition
     */
    val isDefinition: Boolean

    /**
     * Meta-data comprises the file and location in file for the
     * top-level entity.
     */
    val meta: Meta
    fun withMeta(meta: Meta): UnitDeclaration<E, S>

    /**
     * Storage for the top-level entity (e.g., default or static).
     */
    val storage: Storage
    fun withStorage(storage: Storage): UnitDeclaration<E, S>

    val tlid: TLID get() = TLID(ident, kind)
    val kind: EntityKind
    fun mkSymbol(tuid: TUID): Symbol = Symbol(tuid, tlid)

    /* Mixin */

    /** Everything that is visible to other translation units (non-types) */
    sealed interface Valuelike<out E, out S> : UnitDeclaration<E, S> {
        val type: Type
        fun ref() = Exp.Var(ident, type)
        fun prototype() = Prototype(tlid, type)
    }

    /** Everything that declares a new type: Struct, Union, Enum */
    sealed interface TypeDeclaration: UnitDeclaration<Nothing, Nothing> {
        val type: Type.Defined get() = when (this) {
            is Struct  -> Type.Struct(this.tlid, this.attributes)
            is Union   -> Type.Union(this.tlid, this.attributes)
            is Enum    -> Type.Enum(this.tlid, this.attributes)
            is Typedef -> Type.Typedeffed(this.tlid, listOf())
        }
    }

    /** Structs or Unions */
    sealed interface Compound: TypeDeclaration {
        val fields: Option<FieldDecls>
        val structOrUnion: StructOrUnion
        val attributes: List<Attr>

        override val isDefinition get() = fields.isDefined()

        fun <A> foldFields(initial: A, f: (A, Field.Named) -> A): A =
            fields.fold({ initial }) { fs -> fs.foldFields(initial, f) }
    }

    data class Var<out Exp>(
        override val ident: Ident,
        override val type: Type,
        val rhs: Option<Exp> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ) : Valuelike<Exp, Nothing> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        fun withDefinition(exp: @UnsafeVariance Exp) = this.copy(rhs = exp.some())

        override val isDefinition: Boolean get() = rhs.isDefined()
        override val kind: EntityKind get() = EntityKind.Var

        fun toPrototype() = copy(rhs = None)
        fun <E> mapRhs(onExp: (Exp) -> E): Var<E> = Var(ident, type, rhs.map(onExp), storage, meta)
    }

    data class Fun<out Stmt>(
        override val ident: Ident,
        val inline: Boolean,
        val returnType: Type,
        val params: Params,
        val vararg: Boolean           = false,
        val body: Option<Stmt>        = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default,
    ): Valuelike<Nothing, Stmt> {
        constructor(id: Ident, typ: Type.Fun):
                this(id, false, typ.returnType, typ.params, typ.vararg)
        constructor(id: Ident, typ: Type.Fun, body: Stmt):
                this(id, false, typ.returnType, typ.params, typ.vararg, body.some())

        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        fun withDefinition(stmt: @UnsafeVariance Stmt) = this.copy(body = stmt.some())

        override val type get(): Type = Type.Fun(returnType, params, vararg)

        override val isDefinition: Boolean get() = body.isDefined()

        override val kind: EntityKind get() = EntityKind.Fun

        fun toPrototype(): Fun<Nothing> =
            Fun(ident, inline, returnType, params, vararg, none(), storage, meta)

        fun <S> mapBody(f: (body: Stmt) -> S): Fun<S> =
            Fun(ident, inline, returnType, params, vararg, body.map(f), storage, meta)
    }

    data class Struct(
        override val ident: Ident,
        override val attributes: List<Attr>,
        override val fields: Option<FieldDecls>,
        override val storage: Storage,
        override val meta: Meta
    ): Compound {
        constructor(ident: Ident, fields: Option<FieldDecls>): this(ident, listOf(), fields, Storage.Default, Meta.default)

        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override val kind = EntityKind.Struct
        override val structOrUnion = StructOrUnion.Struct
    }

    data class Union(
        override val ident: Ident,
        override val attributes: List<Attr>,
        override val fields: Option<FieldDecls>,
        override val storage: Storage,
        override val meta: Meta
    ): Compound {
        constructor(ident: Ident, fields: Option<FieldDecls>): this(ident, listOf(), fields, Storage.Default, Meta.default)
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        override val isDefinition: Boolean = fields.isDefined()
        override val kind = EntityKind.Union
        override val structOrUnion = StructOrUnion.Union
    }

    data class Typedef(
        override val ident: Ident,
        val underlyingType: Type,
        override val storage: Storage,
        override val meta: Meta
    ): UnitDeclaration<Nothing, Nothing>, TypeDeclaration {
        constructor(ident: Ident, underlyingType: Type): this(ident, underlyingType, Storage.Default, Meta.default)
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        /** There is no such thing as a typedecl.
         *  A typedef without a type implicitly means typedef int <name>;
         */
        override val isDefinition = true

        override val kind: EntityKind get() = EntityKind.Typedef
    }

    data class Enum(
        override val ident: Ident,
        val attributes: List<Attr>,
        val enumerators: Option<Enumerators>,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): UnitDeclaration<Nothing, Nothing>, TypeDeclaration {
        constructor(ident: Ident): this(ident, listOf(), None)
        constructor(ident: Ident, enumerators: Enumerators): this(ident, listOf(), enumerators.some())
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
data class TranslationUnit<out E, out T> (
    /** The translation unit identifier */
    val tuid : TUID,

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
     * The type environment generated by this translation uniot.
     */
    fun typeEnv(builtins: Builtins) = object:TypeEnv {
        override val builtins: Builtins
            get() = builtins

        override fun lookup(tlid: TLID): Either<TypeEnv.Missing, UnitDeclaration.TypeDeclaration> =
            typeDeclarations
                .find { it.tlid == tlid && it.isDefinition }
                .toOption()
                .toEither { TypeEnv.Missing(tlid) }
    }
}

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
