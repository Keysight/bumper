package com.riscure.langs.c.pp

import arrow.core.getOrElse
import com.riscure.langs.c.ast.*

/* Something we can write string output to */
fun interface Printer {
    fun write(s: String): Unit
}

/* The thing that writes to a printer */
typealias Writer = (output : Printer) -> Unit

/* Sequential composition of writers */
infix fun Writer.andThen(then: Writer): Writer = { w -> this(w); then(w) }
val empty: Writer = {}
fun text(s: String): Writer = { it.write(s) }

fun sequence(writers: Iterable<Writer>, separator: Writer = empty): Writer =
    sequence(writers.iterator(), separator)

private fun sequence(writers: Iterator<Writer>, separator: Writer = empty): Writer =
    if (writers.hasNext()) {
        val w = writers.next()

        if (!writers.hasNext()) w
        else (w andThen separator andThen sequence(writers))
    } else empty

/* The factory interface for producing writers for different AST nodes */
interface WriterFactory {
    fun print(kind: IKind): Writer
    fun print(kind: FKind): Writer

    /* Print a type in standalone form. I.e., when not part of a variable declaration */
    fun print(type: Type): Writer

    /* Print the type prefix of a variable declaration */
    fun printTypePrefix(type: Type): Writer
    /* Print the type suffix of a variable declaration */
    fun printTypeSuffix(type: Type): Writer
    /* Print a variable declaration */
    fun printDecl(ident: String, type: Type): Writer =
        printTypePrefix(type) andThen text(" ${ident}") andThen printTypeSuffix(type)

    fun print(unit: TranslationUnit): Writer =
        sequence(unit.decls.map { print(it) }, separator = text("\n"))

    fun print(toplevel: TopLevel): Writer
    fun printPrototype(thefun: TopLevel.Fun): Writer
}

/**
 * A class that knows how to text ASTs as long as you provide
 * the method for writing the bodies of top-level definitions.
 */
class AstWriters(val bodyWriter: (toplevel: TopLevel) -> Writer): WriterFactory {

    override fun print(kind: IKind): Writer = when(kind) {
        IKind.IBoolean -> text("bool")
        IKind.IChar    -> text("char")
        IKind.ISChar   -> text("signed char")
        IKind.IUChar   -> text("unsigned char")
        IKind.IShort   -> text("short")
        IKind.IUShort  -> text("unsigned short")
        IKind.IInt     -> text("int")
        IKind.IUInt    -> text("unsigned int")
        IKind.ILong    -> text("long")
        IKind.IULong   -> text("unsigned long")
        IKind.ILongLong -> text("long long")
        IKind.IULongLong -> text("unsigned long long")
    }
    override fun print(kind: FKind): Writer = TODO()
    override fun print(type: Type): Writer = when (type) {
        is Type.Array -> print(type.type) andThen text("*")
        else          -> printTypePrefix(type)
    }

    override fun printTypePrefix(type: Type): Writer = when (type) {
        is Type.Array  -> printTypePrefix(type.type)
        is Type.Enum   -> text(type.id)
        is Type.Float  -> print(type.kind)
        is Type.Fun    -> TODO()
        is Type.Int    -> print(type.kind)
        is Type.Named  -> text(type.id)
        is Type.Ptr    -> print(type.type) andThen text("*")
        is Type.Struct -> text("struct ${type.id}")
        is Type.Union  -> text("union ${type.id}")
        is Type.Void   -> text("void")
    }

    override fun printTypeSuffix(type: Type): Writer = when (type) {
        is Type.Array  -> printTypeSuffix(type.type) andThen text("[${type.size.getOrElse { "" }}]")
        else           -> empty
    }

    override fun printPrototype(thefun: TopLevel.Fun): Writer = (
        print(thefun.ret)
            andThen text(" ${thefun.name}")
            andThen text("(")
            andThen (sequence(thefun.params.map { printDecl(it.name, it.type) }, separator = text(", ")))
            andThen text(")")
    )

    override fun print(toplevel: TopLevel) = when (toplevel) {
        is TopLevel.Var -> (
            printDecl(toplevel.name, toplevel.type)
                andThen (
                    if (toplevel.isDefinition) (text(" = ") andThen bodyWriter(toplevel) andThen text(";"))
                    else text(";")
                ))
        is TopLevel.Fun -> (
            printPrototype(toplevel)
                andThen (
                    if (toplevel.isDefinition) (
                        text (" {\n")
                        andThen bodyWriter(toplevel)
                        andThen text("}")
                    ) else text(";")
                ))
        is TopLevel.Typedef -> (
            text("typedef ")
                andThen print(toplevel.typ)
                andThen text("${toplevel.name};")
            )
        is TopLevel.Composite -> (
            text("struct ${toplevel.name}")
                andThen bodyWriter(toplevel)
                andThen text(";")
            )
        is TopLevel.EnumDef -> TODO()
    }

}