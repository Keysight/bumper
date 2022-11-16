// This is a fully type-annotated C source AST.
// The representation is ported from the CompCert CParser elaborated AST.
package com.riscure.bumper.ast

import arrow.core.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import java.nio.file.Path

/**
 * Indicates whether a declaration is scoped at the level of the translation unit,
 * or in some local scope.
 */
enum class Visibility { TUnit, Local }

/** The type of raw identifiers */
typealias Ident    = String

/**
 * The type of references to something (in practice T:Declaration)
 *
 * @property byName indicates the name by which the thing was referenced.
 * @property symbol indicates to what declaration the name resolved.
 */
data class Ref(val byName: Ident, val resolution: Symbol)

/** A source location */
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
    data class Typedeffed (
        val ref: Ref,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Struct (
        val ref: Ref,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Union (
        val ref: Ref,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }
    data class Enum (
        val ref: Ref,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
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
        val elementType: Type,
        override val attrs: Attrs = listOf()
    ): Type() {
        override fun withAttrs(attrs: Attrs): Type = copy(attrs = attrs)
    }

    /* A distinguished type for inline compound type declarations */
    data class InlineDeclaration (
        val declaration: Declaration.TypeDeclaration,
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
        val double = Float(FKind.FDouble)
        @JvmStatic
        val longdouble = Float(FKind.FLongDouble)
        @JvmStatic
        val float = Float(FKind.FFloat)

        @JvmStatic
        fun array(el: Type, size: Option<Long>) = Array(el, size)

        @JvmStatic
        fun function(returns: Type, vararg params: Param, variadic: Boolean = false) =
            Fun(returns, params.toList(), variadic)

    }
}

/* Struct or union field */
data class Field(
    val site: Site,
    val name: Option<Ident>,
    val type: Type,
    val bitfield: Option<Int> = none()
) {
    val isAnonymous: Boolean get() = name.isEmpty()
}

enum class StructOrUnion { Struct, Union }

typealias FieldDecls  = List<Field>

data class Param(val site: Site, val name: Option<Ident> = None, val type: Type) {
    val isAnonymous: Boolean get() = name.isEmpty()
}
typealias Params = List<Param>

data class Enumerator(val name: Ident, val key: Long)
typealias Enumerators = List<Enumerator>

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
    /**
     * The [site] is a path from the root of the translation unit to this declaration.
     * It uniquely identifies a declaration within the translation unit.
     * Unlike the TLID, it can be used to distinguish between different declarations.
     */
    val site: Site

    /**
     * Optional identifier, because some declarations can be anonymous.
     */
    val ident: Option<Ident>
    val isAnonymous get() = ident.isEmpty()

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

    /**
     * How the declaration is scoped.
     */
    val visibility: Visibility
        get() = when {
        site.isLocal()   -> Visibility.Local
        else             -> Visibility.TUnit
    }

    val tlid: Option<TLID> get() = ident.map { TLID(it, kind) }
    val kind: EntityKind

    fun mkSymbol(tuid: TUID): Option<Symbol> = tlid.map { Symbol(tuid, it, site) }

    /* Mixin */

    /** Everything that can be used as a type */
    sealed interface Typelike

    /** Everything that declares a new type: Struct, Union, Enum */
    sealed interface TypeDeclaration: Declaration<Nothing, Nothing>, Typelike

    data class Var<out Exp>(
        override val site: Site,
        val name: Ident,
        val type: Type,
        val rhs: Option<Exp> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Exp, Nothing> {
        override val ident get() = name.some()
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        override val isDefinition: Boolean get() = rhs.isDefined()
        override val kind: EntityKind get() = EntityKind.Var
    }

    data class Fun<out Stmt>(
        override val site: Site,
        val name: Ident,
        val inline: Boolean,
        val returnType: Type,
        val params: Params,
        val vararg: Boolean           = false,
        val body: Option<Stmt>        = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default,
    ): Declaration<Nothing, Stmt> {
        override val ident get() = name.some()
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        fun type(): Type = Type.Fun(returnType, params, vararg)

        override val isDefinition: Boolean get() = body.isDefined()

        override val kind: EntityKind get() = EntityKind.Fun
    }

    /**
     * Struct or Union declaration or definition.
     */
    data class Composite(
        override val site: Site,
        override val ident: Option<Ident>,
        val structOrUnion: StructOrUnion,
        val fields: Option<FieldDecls>      = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, TypeDeclaration {
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

    data class Typedef(
        override val site: Site,
        override val ident: Option<Ident>,
        val underlyingType: Type,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike {
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)

        /** There is no such thing as a typedecl.
         *  A typedef without a type implicitly means typedef int <name>;
         */
        override val isDefinition = true

        override val kind: EntityKind get() = EntityKind.Typedef
    }

    data class Enum(
        override val site: Site,
        override val ident: Option<Ident>,
        val enumerators: Option<Enumerators> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ): Declaration<Nothing, Nothing>, Typelike, TypeDeclaration {
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


/**
 * Paths leading into declarations to possible declaration sites in declarations.
 */
data class Site(val breadcrumbs: List<SiteMarker>) {
    fun <R> scope(crumb: SiteMarker, cont: (Site) -> R): R = cont(this + crumb)
    operator fun plus(crumb: SiteMarker) = Site(breadcrumbs + crumb)

    fun isLocal(): Boolean {
        for (crumb in breadcrumbs) {
            if (crumb.isLocal)
                return true
        }

        return false
    }

    fun isBuiltin(): Boolean =
        breadcrumbs[0].toOption()
            .map { it is Builtin }
            .getOrElse { false }

    companion object {
        val root    = Site(listOf())
        val local   = Site(listOf(Local))
        val builtin = Site(listOf(Builtin))
    }

    sealed interface SiteMarker {
        val isLocal get() = false
    }
    object Local: SiteMarker {
        override val isLocal = true
    }

    /**
     * Root site for builtin declarations, like __builtin_va_list
     */
    object Builtin: SiteMarker
    data class Toplevel(
        /**
         * The top-level declaration is indicated with a sparse int key,
         * because they can be anonymous.
         */
        val site: Int
    ): SiteMarker
    object VarType: SiteMarker
    object FunctionReturn: SiteMarker
    object Pointee: SiteMarker
    data class FunctionParam(
        /**
         * The parameter declaration is indicated with a sparse int key,
         * because they can be anonymous.
         */
        val site: Int
    ): SiteMarker {
        override val isLocal = true
    }
    data class Member(
        /**
         * The member declaration is indicated with a sparse int key,
         * because they can be anonymous.
         */
        val site: Int
    ): SiteMarker
    object Typedef: SiteMarker
}

data class TranslationUnit<out E, out T>(
    val tuid: TUID,

    /** The declarations at the top-level of the file. */
    val toplevelDeclarations: List<Declaration<E, T>>,

    /** All declarations with unit-level scope */
    val declarations: List<Declaration<E, T>>,

    /** Resolution of declarations to definitions. */
    val definitions: Map<Symbol, Symbol>
) {
    /**
     * Find a top-level element in this translation unit, based
     * on a given [TLID] [id].
     */
    /*
    operator fun get(id: TLID): Option<Declaration<E, T>> = decls
        .find { it.ident.exists { it == id.name } && it.ofKind(id.kind) }
        .toOption() // FIXME should also find inline declarations.
     */

    val variables: List<Declaration.Var<E>> get()  = declarations.variables
    val functions: List<Declaration.Fun<T>> get()  = declarations.functions
    val structs: List<Declaration.Composite> get() = declarations.structs

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

    /* Map locations to top-level declarations */
    fun getAtLocation(loc: Location): Option<Declaration<E, T>> =
        toplevelDeclarations
            .find { decl -> decl.meta.location.exists { it.begin == loc }}
            .toOption()

    /* Map locations to top-level declarations */
    fun getEnclosing(range: SourceRange): Option<Declaration<E, T>> =
        toplevelDeclarations
            .find { decl -> decl.meta.location.exists { it.encloses(range) }}
            .toOption()

    /* Map locations to top-level declarations */
    fun getEnclosing(loc: Location): Option<Declaration<E, T>> =
        toplevelDeclarations
            .find { decl -> decl.meta.location.exists { it.encloses(loc) }}
            .toOption()


    /*
    // Indexing a translation unit
    val symbols: List<Symbol> get() = TODO("Gotta fix them symbols") // decls.map { tl -> Symbol(tuid, tl.tlid) }
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

typealias ErasedDeclaration     = Declaration<Any?, Any?>
typealias ErasedTranslationUnit = TranslationUnit<Any?, Any?>

/**
 * Forget the expressions/statements at the leaves.
 */
fun <E,T> TranslationUnit<E, T>.erase(): ErasedTranslationUnit = this


fun <E,T> TranslationUnit<E, T>.update(id: TLID, f: (decl: Declaration<E, T>) -> Declaration<E, T>) =
    copy(toplevelDeclarations = toplevelDeclarations.map { if (it.tlid.exists{ it == id }) f(it) else it })

/* filters for toplevel declarations */

val <E, T> List<Declaration<E, T>>.variables: List<Declaration.Var<E>> get() =
    filterIsInstance<Declaration.Var<E>>()

val <E, T> List<Declaration<E, T>>.functions: List<Declaration.Fun<T>> get() =
    filterIsInstance<Declaration.Fun<T>>()

val <E,T> List<Declaration<E, T>>.structs: List<Declaration.Composite> get() =
    filterIsInstance<Declaration.Composite>()
        .filter { it.structOrUnion == StructOrUnion.Struct }

val <T> List<Declaration.Fun<T>>.definitions: List<Declaration.Fun<T>> get() =
    filter { it.isDefinition }

val <T> List<Declaration.Fun<T>>.declarations: List<Declaration.Fun<T>> get() =
    filter { !it.isDefinition }

val <E, T> List<Declaration<E, T>>.typelike: List<Declaration.Typelike> get() =
    filterIsInstance<Declaration.Typelike>()