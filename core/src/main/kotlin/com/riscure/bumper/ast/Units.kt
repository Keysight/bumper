package com.riscure.bumper.ast

import arrow.core.None
import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import com.riscure.bumper.pp.Pretty
import com.riscure.bumper.serialization.OptionAsNullable
import kotlinx.serialization.Serializable

/**
 * Encompassing any declaration that has a global name (so not including e.g. struct declarations in functions)
 */
@Serializable
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
 * The type of declarations that can be defined at the top of a translation unit.
 *
 * @param E the type that represents expressions
 * @param S the type that represents statements
 */
@Serializable
sealed interface UnitDeclaration<out E, out S> : GlobalDeclaration {

    override fun withIdent(id: Ident): UnitDeclaration<E, S>

    /**
     * Whether this declaration is also a definition.
     *
     * For functions, we can only have one definition per unit,
     * but for globals, we may have multiple (but only one with an initializer).
     *
     * @see valueLikeDefinitions
     */
    val isDefinition: Boolean

    fun erase(): UnitDeclaration<Unit, Unit>

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
    @Serializable
    sealed interface Valuelike<out E, out S> : UnitDeclaration<E, S> {
        val type: Type
        fun ref() = Exp.Var(ident, type)

        val prototype: UnitDeclaration.Valuelike<Nothing, Nothing>

        override fun erase() = when (this) {
            is Fun -> this.mapBody {}
            is Var -> this.mapRhs {}
        }
    }

    /** Everything that declares a new type: Struct, Union, Enum */
    sealed interface TypeDeclaration: UnitDeclaration<Nothing, Nothing> {
        val type: Type.Defined
            get() = when (this) {
            is Struct  -> Type.Struct(this.tlid, this.attributes)
            is Union   -> Type.Union(this.tlid, this.attributes)
            is Enum    -> Type.Enum(this.tlid, this.attributes)
            is Typedef -> Type.Typedeffed(this.tlid, listOf())
        }

        override fun erase() = this
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

    @Serializable
    data class Var<out Exp>(
        override val ident: Ident,
        override val type: Type,
        val rhs: OptionAsNullable<Exp> = None,
        override val storage: Storage = Storage.Default,
        override val meta: Meta = Meta.default
    ) : Valuelike<Exp, Nothing> {
        override fun withIdent(id: Ident) = this.copy(ident = id)
        override fun withMeta(meta: Meta) = this.copy(meta = meta)
        override fun withStorage(storage: Storage) = this.copy(storage = storage)
        fun withDefinition(exp: @UnsafeVariance Exp) = this.copy(rhs = exp.some())

        // An extern global variable with initialization is seen as a defined symbol by linker
        override val isDefinition: Boolean get() = rhs.isDefined() || storage !is Storage.Extern
        override val kind: EntityKind get() = EntityKind.Var

        override val prototype: Var<Nothing> get() = Var(ident, type, none(), storage, meta)
        fun <E> mapRhs(onExp: (Exp) -> E): Var<E> = Var(ident, type, rhs.map(onExp), storage, meta)
    }

    @Serializable
    data class Fun<out Stmt>(
        override val ident: Ident,
        val inline: Boolean,
        val returnType: Type,
        val params: Params,
        val vararg: Boolean           = false,
        val body: OptionAsNullable<Stmt> = None,
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

        override val type get() = Type.Fun(returnType, params, vararg)
        fun withType(ty: Type.Fun) = copy(
            returnType = ty.returnType, params = ty.params, vararg = ty.vararg
        )

        override val isDefinition: Boolean get() = body.isDefined()

        override val kind: EntityKind get() = EntityKind.Fun

        override val prototype: Fun<Nothing> get() =
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
        constructor(ident: Ident, fields: Option<FieldDecls>): this(ident, listOf(), fields,
                                                                    Storage.Default, Meta.default
        )

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
        constructor(ident: Ident, fields: Option<FieldDecls>): this(ident, listOf(), fields,
                                                                    Storage.Default, Meta.default
        )
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
        constructor(ident: Ident, underlyingType: Type): this(ident, underlyingType,
                                                              Storage.Default, Meta.default
        )
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

fun UnitDeclaration<Exp,Stmt>.show() = Pretty.unitDeclaration(this)
