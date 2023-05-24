@file:Suppress("NAME_SHADOWING")

package com.riscure.bumper.ast

import arrow.core.getOrElse


interface Visitor<C> {
    operator fun invoke(acc: C, exp: Exp): C  = acc
    operator fun invoke(acc: C, stmt: Stmt): C = acc

}

fun <C> Initializer.fold(acc: C, visitor: (C, Exp) -> C): C = when (this) {
    is Initializer.InitArray  -> this.exps.fold(acc) { acc, e -> e.fold(acc, visitor) }
    is Initializer.InitStruct -> this.initializers.values.fold(acc) { acc, s -> s.fold(acc, visitor) }
    is Initializer.InitUnion  -> this.initializer.fold(acc) { acc, s -> s.fold(acc, visitor) }
    is Initializer.InitSingle -> this.exp.fold(acc, visitor::invoke)
}

fun <C> Exp.fold(acc: C, visitor: (C, Exp) -> C): C = when (this) {
    is Exp.Alignof -> visitor(acc, this)
    is Exp.BinOp ->
        this.left.fold(acc, visitor)
            .let { acc -> this.right.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Exp.Call ->
        this.funRef.fold(acc, visitor)
            .let { acc -> this.args.fold(acc) { acc, e -> e.fold(acc, visitor) }}
            .let { acc -> visitor(acc, this) }
    is Exp.Cast ->
        this.exp.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Compound ->
        this.initializer.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Conditional ->
        this.condition.fold(acc, visitor)
            .let { acc -> this.thenBranch.fold(acc, visitor) }
            .let { acc -> this.elseBranch.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Exp.Const -> visitor(acc, this)
    is Exp.Sizeof -> visitor(acc, this)
    is Exp.UnOp ->
        this.operand.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Var -> visitor(acc, this)
}

fun <C> List<Stmt>.fold(acc: C, visitor: Visitor<C>): C =
    this.fold(acc) { acc, s -> s.fold(acc, visitor) }

fun <C> Stmt.fold(acc: C, visitor: Visitor<C>): C = when (this) {
    Stmt.Break -> visitor(acc, this)
    Stmt.Continue -> visitor(acc, this)
    Stmt.Skip -> visitor(acc, this)

    is Stmt.Block ->
        stmts.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Stmt.Conditional ->
        condition
            .fold(acc, visitor::invoke)
            .let { acc -> thenBranch.fold(acc, visitor) }
            .let { acc -> elseBranch.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Decl ->
        init
            .map { it.fold(acc, visitor::invoke) }
            .getOrElse { acc }
    is Stmt.Do ->
        this.todo
            .fold(acc, visitor::invoke)
            .let { acc -> visitor(acc, this) }
    is Stmt.DoWhile ->
        this.condition
            .fold(acc, visitor::invoke)
            .let { acc -> body.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.For ->
        this.before
            .fold(acc, visitor)
            .let { acc -> this.condition.fold(acc, visitor::invoke) }
            .let { acc -> this.after.fold(acc, visitor::invoke) }
            .let { acc -> this.body.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Goto ->
        visitor(acc, this)
    is Stmt.Labeled ->
        this.stmt.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Stmt.Return ->
        this.value
            .map { it.fold(acc, visitor::invoke) }.getOrElse { acc }
            .let { acc -> visitor(acc, this) }
    is Stmt.Seq ->
        this.first.fold(acc, visitor)
            .let { acc -> this.second.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Switch ->
        this.scrutinee.fold(acc, visitor::invoke)
            .let { acc -> this.body.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.While ->
        this.condition.fold(acc, visitor::invoke)
            .let { acc -> this.body.fold(acc, visitor) }
            .let { acc -> visitor(acc, this) }
}

typealias TypeVisitor<C> = (acc: C, type: Type) -> C

fun <C> Type.fold(acc: C, visitor: TypeVisitor<C>): C = when (this) {
    is Type.Array ->
        this.elementType.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Type.Struct -> visitor(acc, this)
    is Type.Union -> visitor(acc, this)
    is Type.Enum -> visitor(acc, this)
    is Type.Typedeffed -> visitor(acc, this)
    is Type.Float -> visitor(acc, this)
    is Type.Int -> visitor(acc, this)
    is Type.Fun ->
        this.returnType
            .fold(acc, visitor)
            .let { acc -> params.fold(acc) { acc, param -> param.type.fold(acc, visitor) }}
    is Type.Ptr ->
        this.pointeeType.fold(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Type.Void -> visitor(acc, this)
    is Type.VaList -> visitor(acc, this)
}

/**
 * Fold over a field, visiting any type that we encounter.
 */
fun <C> Field.fold(acc: C, visitor: TypeVisitor<C>): C = when (this) {
    is Field.Anonymous -> subfields.fold(acc) { acc, field ->
        field.fold(acc, visitor)
    }
    is Field.Named     -> visitor(acc, this.type)
}

/**
 * Fold over a type declaration, visiting any type that we encounter.
 */
fun <C> UnitDeclaration.TypeDeclaration.fold(acc: C, visitor: TypeVisitor<C>) = when (this) {
    is UnitDeclaration.Enum    -> acc
    is UnitDeclaration.Struct  -> fields.fold({ acc }) { fs ->
        fs.fold(acc) { acc, field ->
            field.fold(acc, visitor)
        }
    }
    is UnitDeclaration.Union   -> fields.fold({ acc }) { fs ->
        fs.fold(acc) { acc, field ->
            field.fold(acc, visitor)
        }
    }
    is UnitDeclaration.Typedef -> visitor(acc, underlyingType)
}