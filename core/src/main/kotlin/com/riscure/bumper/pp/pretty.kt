package com.riscure.bumper.pp

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.BinaryOp.*
import com.riscure.bumper.ast.UnaryOp.*
import com.riscure.bumper.index.TUID

/**
 * Total pretty printing functions.
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
    fun declaration(ident: Ident, type: Type): String {
        val (remainingType, decl) = namePart(type, ident)
        return "${maybeAttrs(remainingType.attrs)}${typePrefix(remainingType)} $decl".trim()
    }

    @JvmStatic
    fun type(t: Type) = declaration("", t)

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
        is Type.Float             -> floatKind(type.kind)
        is Type.Int               -> integerKind(type.kind)
        is Type.Typedeffed        -> type.ref.name
        is Type.Struct            -> "struct ${type.ref.name}"
        is Type.Enum              -> "enum ${type.ref.name}"
        is Type.Union             -> "union ${type.ref.name}"
        is Type.Void              -> "void"

        is Type.Complex           -> "${floatKind(type.kind)} _Complex"
        is Type.Atomic            -> "_Atomic ${typePrefix(type.elementType)}"
    }

    @JvmStatic
    fun signature(function: UnitDeclaration.Fun<*>): String {
        assert(function.returnType !is Type.Array) { "Invariant violation while pretty-printing type" }

        return with(function) {
            signature(ident, params, vararg, returnType)
        }
    }

    /**
     * Print the signature of a function, without trailing semi-colon.
     */
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
    fun struct(struct: UnitDeclaration.Struct) =
        "struct ${maybeName(struct.ident)}${maybeFields(struct.fields)}"

    @JvmStatic
    fun union(union: UnitDeclaration.Union) =
        "union ${maybeName(union.ident)}${maybeFields(union.fields)}"

    @JvmStatic
    fun typedecl(typeDecl: UnitDeclaration.TypeDeclaration) = when (typeDecl) {
        is UnitDeclaration.Typedef   -> typedef(typeDecl)
        is UnitDeclaration.Struct    -> struct(typeDecl)
        is UnitDeclaration.Union     -> union(typeDecl)
        is UnitDeclaration.Enum      -> with(typeDecl) {
            "enum ${maybeName(ident)}${maybeEnumerators(enumerators)}"
        }
    }

    @JvmStatic
    fun lhs(toplevel: UnitDeclaration<*, *>): String = when (toplevel) {
        is UnitDeclaration.Var             -> with(toplevel) { "${storage(storage)} ${declaration(ident, type)}" }
        is UnitDeclaration.Fun             -> with(toplevel) { "${storage(storage)} ${signature(toplevel)}" }
        is UnitDeclaration.TypeDeclaration -> typedecl(toplevel)
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
    fun field(field: Field): String = when (field) {
        is Field.Anonymous -> composite(field.structOrUnion, "", field.subfields.some())
        is Field.Named     -> "${declaration(field.name, field.type)}${bitFieldSpec(field.bitfield)}"
    }

    @JvmStatic
    fun fields(fields: List<Field>) = fields.joinToString(separator="; ") { field(it) }

    @JvmStatic
    fun line(loc: Location) = "#line ${loc.row} ${loc.sourceFile}"

    private fun maybeInit(i: Option<Initializer>): String =
        i.map { " = ${initializer(it)}" }.getOrElse { "" }

    @JvmStatic
    fun initializer(i: Initializer): String = when (i) {
        is Initializer.InitArray  -> TODO()
        is Initializer.InitStruct -> TODO()
        is Initializer.InitUnion  -> TODO()
        is Initializer.InitSingle -> exp(i.exp)
    }

    @JvmStatic
    fun stmt(s: Stmt): String = when (s) {
        is Stmt.Block ->
            """
            {
                ${stmt(Stmt.seq(s.stmts))};
            }
            """
        is Stmt.Break -> "break;"
        is Stmt.Conditional ->
            """
            if (${exp(s.condition)}) {
                ${stmt(s.thenBranch)}
            } ${if (s.elseBranch is Stmt.Skip) "" else stmt(s.elseBranch)}
            """.trimIndent()
        is Stmt.Continue -> "continue;"
        is Stmt.Decl -> with(s) {
            "${storage(storage)} ${declaration(ident, type)}${maybeInit(init)};"
        }
        is Stmt.Do -> "${exp(s.todo)};"
        is Stmt.DoWhile -> TODO()
        is Stmt.For ->
            """
            for (${stmt(s.before)} ${exp(s.condition)}; ${exp(s.after)}) {
                ${stmt(s.body)}
            }
            """.trimIndent()
        is Stmt.Goto -> TODO()
        is Stmt.Labeled ->
            """
            ${s.label}:
                ${stmt(s.stmt)}
            """.trimIndent()

        is Stmt.Return -> when (val exp = s.value) {
            is Some -> "return ${exp(exp.value)};"
            is None -> "return;"
        }
        is Stmt.Seq -> {
            val fst = stmt(s.first)
            val snd = stmt(s.second)
            when {
                fst.isNotBlank() && snd.isNotBlank() -> "$fst\n$snd"
                fst.isNotBlank()                     -> fst
                snd.isNotBlank()                     -> snd
                else                                 -> ""
            }
        }
        is Stmt.Switch ->
            """
            switch (${exp(s.scrutinee)}) {
                ${stmt(s.body)}
            }
            """.trimIndent()
        is Stmt.While -> TODO()
        Stmt.Skip -> ""
    }

    @JvmStatic
    fun exp(e: Exp, atPrec: Int = 0): String {
        // The precedence computation is complete magic.
        // We did not invent this ourselves, but just reused CompCert's logic.
        // see https://github.com/AbsInt/CompCert/blob/994c6c34182606385140e5695e33c90507ce59ee/cparser/Cprint.ml
        // TODO We need to open source this component to satisfy CompCerts LGPL!
        val prec = e.prec.prec
        val (prec1, prec2) =
            if (e.prec.assoc == Assoc.LR)
                Pair(prec, prec + 1)
            else
                Pair(prec + 1, prec)

        val ePrint = when (e) {
            is Exp.Alignof -> TODO()
            is Exp.Call -> {
                val args = e.args.joinToString(separator = ", ") { exp(it, 2) };

                when (e.funRef) {
                    is Exp.Var -> "${exp(e.funRef, prec)}($args)"
                    else       -> "(${exp(e.funRef, prec)})($args)"
                }
            }
            is Exp.Cast -> TODO()
            is Exp.Compound -> TODO()
            is Exp.Conditional -> TODO()
            is Exp.Const -> when (val c = e.constant) {
                is Constant.CInt   -> c.value.toString()
                is Constant.CFloat -> TODO()
                is Constant.CStr   -> TODO()
            }
            is Exp.Sizeof -> "sizeof(${type(e.type)})"
            is Exp.Var -> e.name
            is Exp.BinOp -> {
                val l = exp(e.left, prec1)
                val r = exp(e.right, prec2)
                when (e.op) {
                    OAdd -> "$l + $r"
                    OSub -> "$l - $r"
                    OMul -> "$l * $r"
                    ODiv -> "$l / $r"
                    OMod -> "$l % $r"
                    OAnd -> "$l & $r"
                    OLogAnd -> "$l && $r"
                    OOr -> "$l | $r"
                    OLogOr -> "$l || $r"
                    OXor -> "$l ^ $r"
                    OShl -> "$l << $r"
                    OShr -> "$l >> $r"
                    OEq -> "$l == $r"
                    ONe -> "$l != $r"
                    OLt -> "$l < $r"
                    OGt -> "$l > $r"
                    OLe -> "$l <= $r"
                    OGe -> "$l >= $r"
                    OIndex -> "$l[${exp(e.right, 0)}]"
                    OAssign -> "$l = $r"
                    OAddAssign -> "$l += $r"
                    OSubAssign -> "$l -= $r"
                    OMulAssign -> "$l *= $r"
                    ODivAssign -> "$l /= $r"
                    OModAssign -> "$l %= $r"
                    OAndAssign -> "$l &= $r"
                    OOrAssign -> "$l |= $r"
                    OXorAssign -> "$l ^= $r"
                    OShlAssign -> "$l <<= $r"
                    OShrAssign -> "$l >>= $r"
                    OComma -> "$l, $r"
                }
            }
            is Exp.UnOp  -> {
                val l = exp(e.operand, prec)
                when (e.op) {
                    is OArrow -> "$l->${e.op.member}"
                    is ODot -> "$l.${e.op.member}"
                    OAddrOf -> "&$l"
                    ODeref -> "*$l"
                    OLogNot -> "!$l"
                    OMinus -> "-$l"
                    ONot -> "~$l"
                    OPlus -> "+$l"
                    OPostDecr -> "$l--"
                    OPostIncr -> "$l++"
                    OPreDecr -> "--$l"
                    OPreIncr -> "++$l"
                }
            }
        }

        return if (prec < atPrec) "($ePrint)" else ePrint
    }

    @JvmStatic
    fun unitDeclaration(decl: UnitDeclaration<Exp, Stmt>): String =
        lhs(decl) + when (decl) {
            is UnitDeclaration.TypeDeclaration -> ";"
            is UnitDeclaration.Var -> decl.rhs .map { "= ${exp (it)};" }.getOrElse { ";" }
            is UnitDeclaration.Fun -> decl.body.map { "{\n${stmt(it)}\n}" }.getOrElse { ";" }
        }

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
            is Some -> stmtWriter(stmt.value).map { text("{\n$it\n}") }
            is None -> semicolon.right()
        }

        is UnitDeclaration.TypeDeclaration -> semicolon.right()
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

        /**
         * An instance of AstWriters for translation units whose expressions and statements
         * are C expression/statements ASTs.
         */
        @JvmStatic
        fun usingPretty(): AstWriters<Exp, Stmt> = AstWriters({ Pretty.exp(it).right() }, { Pretty.stmt(it).right() })

    }
}
