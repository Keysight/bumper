package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.langs.c.ast.*

object Pretty {
    fun print(kind: IKind): String = when(kind) {
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
    fun print(kind: FKind): String = when (kind) {
        FKind.FFloat -> "float"
        FKind.FDouble -> "double"
        FKind.FLongDouble -> "long double"
    }

    fun printDecl(ident: String, type: Type): String {
        val (remainingType, decl) = printNamePart(type, ident)
        return "${printTypePrefix(remainingType)} $decl".trim()
    }

    private fun formals(params: List<Param>): String =
        params.joinToString(separator=", ") { printDecl(it.name, it.type) }
    private fun printNamePart(type: Type, name: String): Pair<Type, String> = when (type) {
        is Type.Ptr   -> printNamePart(type.pointeeType, "*${name}")
        is Type.Fun   -> {
            Pair(type.returnType, "($name)(${formals(type.params)})")
        }
        is Type.Array -> printNamePart(type.elementType, "$name[${type.size.getOrElse { "" }}]")
        else          -> Pair(type, name)
    }

    private fun printTypePrefix(type: Type): String = when (type) {
        // This part should have been printed by the name-part printer
        is Type.Array  ->
            throw RuntimeException("Failed to pretty-print ill-formed type.")
        is Type.Fun    ->
            throw RuntimeException("Failed to pretty-print ill-formed type.")

        // Possible remainders:
        is Type.Ptr    -> "${printTypePrefix(type.pointeeType)}*"
        is Type.Enum   -> type.id
        is Type.Float  -> print(type.kind)
        is Type.Int    -> print(type.kind)
        is Type.Named  -> type.id
        is Type.Struct -> "(struct ${type.id})"
        is Type.Union  -> "(union ${type.id})"
        is Type.Void   -> "void"
    }

    fun printPrototype(thefun: TopLevel.Fun): String {
        assert(thefun.returnType !is Type.Array) { "Invariant violation while pretty-printing type" }
        return printDecl("${thefun.name}(${formals(thefun.params)})", thefun.returnType)
    }
}

/* Something we can write string output to */
fun interface Printer {
    fun print(s: String)

    companion object {
        @JvmStatic
        fun of(jwriter: java.io.Writer) = Printer { jwriter.write(it) }
    }
}

/* The thing that writes to a printer */
fun interface Writer {
    fun write(output: Printer)
}

/* Sequential composition of writers */
infix fun Writer.andThen(that: Writer): Writer = Writer { w -> write(w); that.write(w) }
val empty: Writer = Writer { }
fun text(s: String): Writer = Writer { it.print(s) }

fun sequence(writers: Iterable<Writer>, separator: Writer = empty): Writer =
    sequence(writers.iterator(), separator)

private fun sequence(writers: Iterator<Writer>, separator: Writer = empty): Writer =
    if (writers.hasNext()) {
        val w = writers.next()

        if (!writers.hasNext()) w
        else (w andThen separator andThen sequence(writers, separator))
    } else empty

/**
 * A class that knows how to text ASTs as long as you provide
 * the method for writing the bodies of top-level definitions.
 */
class AstWriters(
    /**
     * A factory for writers for top-level entity bodies.
     * It is expected that the bodyWriter includes the whitespace around the rhs's.
     */
    val bodyWriter: (toplevel: TopLevel) -> Either<Throwable, Writer>
) {
    fun print(unit: TranslationUnit): Either<Throwable,Writer> =
        unit.decls
            .map { print(it) }
            .sequence()
            .map { writers -> sequence(writers, separator = text("\n")) }

    fun print(toplevel: TopLevel): Either<Throwable, Writer> = when (toplevel) {
        is TopLevel.Var -> {
            text(Pretty.printDecl(toplevel.name, toplevel.type))
                .right()
                .flatMap { lhs ->
                    val rhs = if (toplevel.isDefinition) {
                        bodyWriter(toplevel).map { body -> text(" =") andThen body andThen text(";") }
                    } else text(";").right()

                    rhs.map { lhs andThen it }
                }
        }

        is TopLevel.Fun -> {
            text(Pretty.printPrototype(toplevel))
                .right()
                .flatMap { lhs ->
                    val rhs = if (toplevel.isDefinition) {
                        bodyWriter (toplevel).map { body -> text(" {") andThen body andThen text("}") }
                    } else text(";").right()

                    rhs.map { lhs andThen it }
                }
        }

        is TopLevel.Typedef -> Either.Right(
            text("typedef ")
                    andThen text(Pretty.printDecl(toplevel.name, toplevel.underlyingType))
        )

        is TopLevel.Composite ->
            bodyWriter(toplevel)
                .map { rhs -> text("struct ${toplevel.name}") andThen rhs andThen text(";") }

        is TopLevel.EnumDef -> TODO()
    }

}