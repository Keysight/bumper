package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.pp.writer.*

/**
 * Total pretty printing functions, mainly for C types.
 */
object Pretty {
    fun integerKind(kind: IKind): String = when(kind) {
        IKind.IBoolean -> "bool"
        IKind.IChar    -> "char"
        IKind.ISChar   -> "signed char"
        IKind.IUChar   -> "unsigned char"
        IKind.IShort   -> "short"
        IKind.IUShort  -> "unsigned short"
        IKind.IInt     -> "int"
        IKind.IUInt    -> "unsigned int"
        IKind.ILong    -> "long"
        IKind.IULong   -> "unsigned long"
        IKind.ILongLong -> "long long"
        IKind.IULongLong -> "unsigned long long"
    }
    fun floatKind(kind: FKind): String = when (kind) {
        FKind.FFloat -> "float"
        FKind.FDouble -> "double"
        FKind.FLongDouble -> "long double"
    }

    fun declaration(ident: Ident, type: Type): String {
        val (remainingType, decl) = namePart(type, ident)
        return "${maybeAttrs(remainingType.attrs)}${typePrefix(remainingType)} $decl".trim()
    }

    private fun typeAttrs(attrs: Attrs) =
        attrs.joinToString(separator=" ") { when (it) {
            is Attr.AlignAs   -> "__attribute__ ((aligned(${it.alignment})))"
            Attr.Constant     -> "const"
            Attr.Restrict     -> "restrict"
            Attr.Volatile     -> "volatile"
            is Attr.NamedAttr -> TODO()
        }}

    private fun formals(params: List<Param>): String =
        params.joinToString(separator=", ") { declaration(it.name, it.type) }

    private fun maybeAttrs(attrs: Attrs) = if (attrs.isNotEmpty()) " ${typeAttrs(attrs)} " else ""

    private fun namePart(type: Type, name: String): Pair<Type, String> = when (type) {
        is Type.Ptr   -> namePart(type.pointeeType, "*${maybeAttrs(type.attrs)}${name}")
        is Type.Fun   -> {
            // this is strange, but true, I think
            namePart(type.returnType, "($name)(${formals(type.params)})")
        }
        is Type.Array -> namePart(type.elementType, "$name[${type.size.getOrElse { "" }}]")
        else          -> Pair(type, name)
    }

    private fun typePrefix(type: Type): String = when (type) {
        // This part should have been printed by the name-part printer
        is Type.Array             -> throw RuntimeException("Failed to pretty-print ill-formed type.")
        is Type.Fun               -> throw RuntimeException("Failed to pretty-print ill-formed type.")

        // Possible remainders:
        is Type.Ptr               -> "${typePrefix(type.pointeeType)}*"
        is Type.Enum              -> type.ref.byName
        is Type.Float             -> floatKind(type.kind)
        is Type.Int               -> integerKind(type.kind)
        is Type.Typedeffed        -> type.ref.byName
        is Type.Struct            -> "struct ${type.ref}" // with parens it doesn't parse
        is Type.Union             -> "union ${type.ref}"  // same.
        is Type.Void              -> "void"

        is Type.Complex           -> "${floatKind(type.kind)} _Complex"
        is Type.Atomic            -> "_Atomic ${typePrefix(type.elementType)}"

        // inline compound declarations are entirely printed prefix.
        is Type.InlineDeclaration -> lhs(type.declaration)
    }

    fun prototype(thefun: Declaration.Fun<*>): String {
        assert(thefun.returnType !is Type.Array) { "Invariant violation while pretty-printing type" }
        return declaration("${thefun.name}(${formals(thefun.params)})", thefun.returnType)
    }

    fun typedef(typedef: Declaration.Typedef): String =
        "typedef ${declaration(typedef.ident.getOrElse { "" }, typedef.underlyingType)}"

    fun storage(storage: Storage): String = when (storage) {
        Storage.Default -> ""
        Storage.Extern  -> "extern"
        Storage.Static  -> "static"
        Storage.Auto    -> "auto"
        Storage.Register -> "register"
    }

    fun lhs(toplevel: Declaration<*,*>): String = when (toplevel) {
        is Declaration.Var                                             -> with(toplevel) { "${storage(storage)} ${declaration(name, type)}" }
        is Declaration.Fun                                             -> with(toplevel) { "${storage(storage)} ${prototype(toplevel)}"     }
        is Declaration.Typedef                                         -> typedef(toplevel)
        is Declaration.Composite                                       -> with(toplevel) {
            when (structOrUnion) {
                StructOrUnion.Struct -> "struct ${maybeName(ident)}${maybeFields(fields)}"
                StructOrUnion.Union -> "union ${maybeName(ident)}${maybeFields(fields)}"
            }
        }
        is Declaration.Enum -> "enum ${maybeName(toplevel.ident)} { TODO } " // TODO
    }

    private fun maybeFields(fields: Option<List<Field>>) = when (fields) {
        is None -> ""
        is Some -> " { ${fields(fields.value)}; }"
    }

    private fun maybeName(ident: Option<Ident>) = if (ident.isEmpty()) "" else "$ident "
    private fun bitFieldSpec(bitfield: Option<Int>): String =
        bitfield
            .map { " : ${it}" }
            .getOrElse {"" }

    fun field(field: Field) = when {
        field.anonymous -> TODO("Can't print anonymous field $field")
        else -> {
            "${declaration(field.name, field.type)}${bitFieldSpec(field.bitfield)}"
        }

    }
    fun fields(fields: List<Field>) = fields.joinToString(separator="; ") { field(it) }
}

/**
 * A class that knows how to text ASTs as long as you provide
 * the method for writing the bodies of top-level definitions.
 */
class AstWriters(
    /**
     * A factory for writers for top-level entity bodies.
     * It is expected that the bodyWriter includes the non-mandatory whitespace around the rhs's.
     */
    val getBodySource: (toplevel: Declaration<*,*>) -> Either<Throwable, String>
) {
    private val semicolon = text(";")

    fun print(unit: ErasedTranslationUnit): Either<Throwable,Writer> =
        unit.decls
            .map { print(it) }
            .sequence()
            .map { writers -> sequence(writers, separator = text("\n")) }

    fun print(toplevel: Declaration<*,*>): Either<Throwable, Writer> =
        rhs(toplevel).map { rhs -> text(Pretty.lhs(toplevel)) andThen rhs }

    fun rhs(toplevel: Declaration<*,*>): Either<Throwable,Writer> = when (toplevel) {
        is Declaration.Var ->
                if (toplevel.isDefinition) {
                    getBodySource(toplevel).map { body -> text(" =$body;") }
                } else {
                    semicolon.right()
                }

        is Declaration.Fun ->
                if (toplevel.isDefinition) {
                    getBodySource(toplevel).map { body -> text(" {$body}") }
                } else {
                    semicolon.right()
                }
        is Declaration.Typedef   -> semicolon.right()
        is Declaration.Composite                                       -> semicolon.right()
        is Declaration.Enum -> semicolon.right()
    }
}