package com.riscure.bumper.pp

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID

/**
 * Total pretty printing functions, mainly for C types.
 */
object Pretty {
    @JvmStatic
    fun integerKind(kind: IKind): String = when (kind) {
        IKind.IBoolean   -> "_Bool"
        IKind.IChar      -> "char"
        IKind.ISChar     -> "signed char"
        IKind.IUChar     -> "unsigned char"
        IKind.IShort     -> "short"
        IKind.IUShort    -> "unsigned short"
        IKind.IInt       -> "int"
        IKind.IUInt      -> "unsigned int"
        IKind.ILong      -> "long"
        IKind.IULong     -> "unsigned long"
        IKind.ILongLong  -> "long long"
        IKind.IULongLong -> "unsigned long long"
    }

    @JvmStatic
    fun floatKind(kind: FKind): String = when (kind) {
        FKind.FFloat      -> "float"
        FKind.FDouble     -> "double"
        FKind.FLongDouble -> "long double"
    }

    @JvmStatic
    fun declaration(ident: Ident, type: FieldType): String {
        val (remainingType, decl) = namePart(type, ident)
        return "${maybeAttrs(remainingType.attrs)}${typePrefix(remainingType)} $decl".trim()
    }

    @JvmStatic
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

    private fun namePart(type: FieldType, name: String): Pair<FieldType, String> = when (type) {
        is Type.Ptr   -> namePart(type.pointeeType, "*${maybeAttrs(type.attrs)}${name}")
        is Type.Fun   -> {
            // this is strange, but true, I think
            namePart(type.returnType, "($name)(${formals(type.params)})")
        }
        is Type.Array -> namePart(type.elementType, "$name[${type.size.getOrElse { "" }}]")
        else          -> Pair(type, name)
    }

    private fun typePrefix(type: FieldType): String = when (type) {
        // This part should have been printed by the name-part printer
        is Type.Array             -> throw RuntimeException("Failed to pretty-print ill-formed type.")
        is Type.Fun               -> throw RuntimeException("Failed to pretty-print ill-formed type.")

        // Possible remainders:
        is Type.Ptr               -> "${typePrefix(type.pointeeType)}*"
        is Type.Float             -> floatKind(type.kind)
        is Type.Int               -> integerKind(type.kind)
        is Type.Typedeffed        -> type.ref.name
        is Type.Struct            -> "struct ${type.ref.name}"
        is Type.Enum              -> "enum ${type.ref.name}"
        is Type.Union             -> "union ${type.ref.name}"
        is Type.Void              -> "void"

        is Type.Complex           -> "${floatKind(type.kind)} _Complex"
        is Type.Atomic            -> "_Atomic ${typePrefix(type.elementType)}"
        is Type.VaList            -> "__builtin_va_list"

        is FieldType.AnonComposite -> with (type) { composite(structOrUnion, "", fields) }
    }

    @JvmStatic
    fun signatureOf(function: UnitDeclaration.Fun<*>): String {
        assert(function.returnType !is Type.Array) { "Invariant violation while pretty-printing type" }

        return with(function) {
            signature(ident, params, vararg, returnType)
        }
    }

    @JvmStatic
    fun signature(ident: Ident, params: Params, vararg: Boolean, returns: Type): String {
        assert(returns !is Type.Array) { "Invariant violation while pretty-printing type" }
        val ppvararg = if (vararg) ", ..." else ""
        return declaration("${ident}(${formals(params)}$ppvararg)", returns)
    }

    @JvmStatic
    fun typedef(typedef: UnitDeclaration.Typedef): String =
        "typedef ${declaration(typedef.ident, typedef.underlyingType)}"

    @JvmStatic
    fun composite(structOrUnion: StructOrUnion, id: Ident, fields: Option<FieldDecls>) = when (structOrUnion) {
        StructOrUnion.Struct -> "struct ${maybeName(id)}${maybeFields(fields)}"
        StructOrUnion.Union  -> "union ${maybeName(id)}${maybeFields(fields)}"
    }

    @JvmStatic
    fun storage(storage: Storage): String = when (storage) {
        Storage.Default  -> ""
        Storage.Extern   -> "extern"
        Storage.Static   -> "static"
        Storage.Auto     -> "auto"
        Storage.Register -> "register"
    }

    @JvmStatic
    fun lhs(toplevel: UnitDeclaration<*, *>): String = when (toplevel) {
        is UnitDeclaration.Var       -> with(toplevel) { "${storage(storage)} ${declaration(ident, type)}" }
        is UnitDeclaration.Fun       -> with(toplevel) { "${storage(storage)} ${signatureOf(toplevel)}" }
        is UnitDeclaration.Typedef   -> typedef(toplevel)
        is UnitDeclaration.Composite -> with(toplevel) {
            composite(structOrUnion, ident, fields)
        }
        is UnitDeclaration.Enum      -> with(toplevel) {
            "enum ${maybeName(ident)}${maybeEnumerators(enumerators)}"
        }
    }

    private fun maybeEnumerators(enums: Option<Enumerators>) = when (enums) {
        is None -> ""
        is Some -> " { ${enums.value.joinToString(separator=", ") { enumerator(it) }} }"
    }

    private fun enumerator(enum: Enumerator) = "${enum.ident} = ${enum.key}"

    private fun maybeFields(fields: Option<List<Field>>) = when (fields) {
        is None -> ""
        is Some -> " { ${fields(fields.value)}; }"
    }

    private fun maybeName(ident: Ident) = "$ident ".trim()

    private fun bitFieldSpec(bitfield: Option<Int>): String =
        bitfield
            .map { " : $it" }
            .getOrElse {"" }

    @JvmStatic
    fun field(field: Field) =
        "${declaration(field.name, field.type)}${bitFieldSpec(field.bitfield)}"

    @JvmStatic
    fun fields(fields: List<Field>) = fields.joinToString(separator="; ") { field(it) }

    @JvmStatic
    fun line(loc: Location) = "#line ${loc.row} ${loc.sourceFile}"
}

/**
 * A class that knows how to text ASTs as long as you provide
 * the method for writing the bodies of top-level definitions.
 */
class AstWriters<Exp, Stmt>(
    val expWriter : (exp: Exp)  -> Either<String, String>,
    val stmtWriter: (stm: Stmt) -> Either<String, String>
) {
    private val semicolon = text(";")

    fun print(unit: TranslationUnit<Exp, Stmt>): Either<String, Writer> =
        unit.declarations
            .map { print(it) }
            .sequence()
            .map { writers -> sequence(writers, separator = text("\n")) }

    fun print(toplevel: UnitDeclaration<Exp, Stmt>): Either<String, Writer> =
        rhs(toplevel).map { rhs ->
            text(Pretty.lhs(toplevel))
                .andThen(rhs)

            /*
            Ideally we also (optionally) output line directives here
            to refer to the "most" original location that is known.
            This also requires us to then reset the line directive for declarations
            that have no location data attached.
            Unfortunately, the C preprocessor has no directive however
            to reset the line info like that.
            Hence, we would have to somehow keep track of the 'current line' that we are outputting.
            This is doable, but requires some refactoring to wire that state data through the writer.

            toplevel.meta
                .presumedLocation
                .or(toplevel.meta.location.map { it.begin })
                .let { maybeLocation ->
                    maybeLocation.fold(
                        { empty },
                        { l -> text(Pretty.line(l)) }
                    )
                }
             */
        }

    fun rhs(toplevel: UnitDeclaration<Exp, Stmt>): Either<String, Writer> = when (toplevel) {
        is UnitDeclaration.Var       -> when (val exp = toplevel.rhs) {
            is Some -> expWriter(exp.value).map { text(" = $it;") }
            is None -> semicolon.right()
        }

        is UnitDeclaration.Fun       -> when (val stmt = toplevel.body) {
            is Some -> stmtWriter(stmt.value).map { text(it) }
            is None -> semicolon.right()
        }

        is UnitDeclaration.Typedef   -> semicolon.right()
        is UnitDeclaration.Composite -> semicolon.right()
        is UnitDeclaration.Enum      -> semicolon.right()
    }

    companion object {

        /**
         * An instance of AstWriters for translation units whose expressions and statements
         * are represented by source ranges. We can use the code extractor to print such
         * ASTs.
         */
        @JvmStatic
        fun usingExtraction(tuid: TUID): AstWriters<SourceRange, SourceRange> {
            val extractor = Extractor(tuid.main.toFile())
            fun rangePrinter(it: SourceRange) = extractor.extract(it)
            return AstWriters(::rangePrinter, ::rangePrinter)
        }

        /**
         * An instance of AstWriters for translation units whose expressions and statements
         * are represented by pretty-printed strings already.
         */
        @JvmStatic
        fun usingStrings(): AstWriters<String, String> = AstWriters({ it.right() }, { it.right() })

    }
}
