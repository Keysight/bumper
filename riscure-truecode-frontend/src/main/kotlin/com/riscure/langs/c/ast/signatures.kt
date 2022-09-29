// This is a fully type-annotated C source AST.
// The representation is ported from the CompCert CParser elaborated AST.
package com.riscure.langs.c.ast

import java.nio.file.Path
import java.util.*

typealias Name  = String
typealias Ident = String
data class Location(val sourceFile: Path, val row: Int, val col: Int)
data class SourceRange(val begin: Location, val end: Location)

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
    data class AlignAs(val alignment: Int) : Attr()
    data class NamedAttr(val name: String, val args: List<AttrArg>) : Attr()
}

typealias Attrs = List<Attr>

/* Types */
sealed class Type {
    abstract val attrs: Attrs

    data class Void   (override val attrs: Attrs = listOf()): Type()
    data class Int    (val kind: IKind, override val attrs: Attrs = listOf()): Type()
    data class Float  (val kind: FKind, override val attrs: Attrs = listOf()): Type()
    data class Ptr    (val type: Type, override val attrs: Attrs = listOf()): Type()
    data class Array  (val type: Type, val size: Optional<Long> = Optional.empty(), override val attrs: Attrs = listOf()): Type()
    data class Fun    (val retType: Type, val args: List<Pair<Ident, Type>>, val vararg: Boolean, override val attrs: Attrs = listOf()): Type()
    data class Named  (val id: Ident, val underlying: Type, override val attrs: Attrs = listOf()): Type()
    data class Struct (val id: Ident, override val attrs: Attrs = listOf()): Type()
    data class Union  (val id: Ident, override val attrs: Attrs = listOf()): Type()
    data class Enum   (val id: Ident, override val attrs: Attrs = listOf()): Type()
}

/* Struct or union field */
data class Field(
    val name: String
  , val type: Type
  , val bitfield: Int?
  , val anonymous: Boolean
)

enum class StructOrUnion { Struct, Union }

data class Param(
    val name: Ident,
    val type: Type
)

typealias FieldDecls = List<Field>
data class Enumerator(val name: Ident, val key: Long) // TODO missing optional exp?

/* TODO fun possibly missing attributes and storage fields, as well as a pragma thingy? */
sealed class TopLevel {
    abstract val name: Ident
    abstract val location: Optional<SourceRange>
    abstract val doc: Optional<String>

    sealed interface Typedecl

    abstract fun withMeta(doc: Optional<String> = Optional.empty(),
                          location: Optional<SourceRange> = Optional.empty()): TopLevel

    data class VarDecl(
        override val name: Ident,
        val type: Type,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ): TopLevel() {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }

    data class FunDecl(
        override val name: Ident,
        val inline: Boolean,
        val ret: Type,
        val params: List<Param>,
        val vararg: Boolean,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ) : TopLevel() {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }

    data class FunDef(
        override val name: Ident,
        val inline: Boolean,
        val ret: Type,
        val params: List<Param>,
        val vararg: Boolean,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ) : TopLevel() {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }

    data class Composite(
        override val name: Ident,
        val structOrUnion: StructOrUnion,
        val fields: FieldDecls,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ): TopLevel(), Typedecl {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }

    data class Typedef(
        override val name: Ident,
        val typ: Type,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ): TopLevel() {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }

    data class EnumDef(
        override val name: Ident,
        val enumerators: List<Enumerator>,
        override val location: Optional<SourceRange> = Optional.empty(),
        override val doc: Optional<String> = Optional.empty()
    ): TopLevel(), Typedecl {
        override fun withMeta(doc: Optional<String>, location: Optional<SourceRange>) =
            this.copy(doc = doc, location = location)
    }
}

data class TranslationUnit(
    val decls: List<TopLevel>
)
