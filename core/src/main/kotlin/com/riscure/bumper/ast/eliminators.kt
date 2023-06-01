@file:Suppress("NAME_SHADOWING")

package com.riscure.bumper.ast

import arrow.core.getOrElse


interface Visitor<C> {
    operator fun invoke(acc: C, exp: Exp): C  = acc
    operator fun invoke(acc: C, stmt: Stmt): C = acc

}

/**
 * Visit all the expressions in this initializer in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> Initializer.bottomUp(acc: C, visitor: (C, Exp) -> C): C = when (this) {
    is Initializer.InitArray  -> this.exps.fold(acc) { acc, e -> e.bottomUp(acc, visitor) }
    is Initializer.InitStruct -> this.initializers.values.fold(acc) { acc, s -> s.bottomUp(acc, visitor) }
    is Initializer.InitUnion  -> this.initializer.bottomUp(acc) { acc, s -> s.bottomUp(acc, visitor) }
    is Initializer.InitSingle -> this.exp.bottomUp(acc, visitor::invoke)
}

/**
 * Visit all the expressions in this expression in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> Exp.bottomUp(acc: C, visitor: (C, Exp) -> C): C = when (this) {
    is Exp.Alignof -> visitor(acc, this)
    is Exp.BinOp ->
        this.left.bottomUp(acc, visitor)
            .let { acc -> this.right.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Exp.Call ->
        this.funRef.bottomUp(acc, visitor)
            .let { acc -> this.args.fold(acc) { acc, e -> e.bottomUp(acc, visitor) }}
            .let { acc -> visitor(acc, this) }
    is Exp.Cast ->
        this.exp.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Compound ->
        this.initializer.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Conditional ->
        this.condition.bottomUp(acc, visitor)
            .let { acc -> this.thenBranch.bottomUp(acc, visitor) }
            .let { acc -> this.elseBranch.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Exp.Const -> visitor(acc, this)
    is Exp.Sizeof -> visitor(acc, this)
    is Exp.UnOp ->
        this.operand.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Exp.Var -> visitor(acc, this)
}

/**
 * Visit all the expressions and statements in this list of statements in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> List<Stmt>.bottomUp(acc: C, visitor: Visitor<C>): C =
    this.fold(acc) { acc, s -> s.bottomUp(acc, visitor) }

/**
 * Visit all the statements in this statement in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> Stmt.bottomUp(acc: C, visitor: Visitor<C>): C = when (this) {
    Stmt.Break -> visitor(acc, this)
    Stmt.Continue -> visitor(acc, this)
    Stmt.Skip -> visitor(acc, this)

    is Stmt.Block ->
        stmts.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Stmt.Conditional ->
        condition
            .bottomUp(acc, visitor::invoke)
            .let { acc -> thenBranch.bottomUp(acc, visitor) }
            .let { acc -> elseBranch.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Decl ->
        init
            .map { it.bottomUp(acc, visitor::invoke) }
            .getOrElse { acc }
    is Stmt.Do ->
        this.todo
            .bottomUp(acc, visitor::invoke)
            .let { acc -> visitor(acc, this) }
    is Stmt.DoWhile ->
        this.condition
            .bottomUp(acc, visitor::invoke)
            .let { acc -> body.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.For ->
        this.before
            .bottomUp(acc, visitor)
            .let { acc -> this.condition.bottomUp(acc, visitor::invoke) }
            .let { acc -> this.after.bottomUp(acc, visitor::invoke) }
            .let { acc -> this.body.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Goto ->
        visitor(acc, this)
    is Stmt.Labeled ->
        this.stmt.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Stmt.Return ->
        this.value
            .map { it.bottomUp(acc, visitor::invoke) }.getOrElse { acc }
            .let { acc -> visitor(acc, this) }
    is Stmt.Seq ->
        this.first.bottomUp(acc, visitor)
            .let { acc -> this.second.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.Switch ->
        this.scrutinee.bottomUp(acc, visitor::invoke)
            .let { acc -> this.body.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
    is Stmt.While ->
        this.condition.bottomUp(acc, visitor::invoke)
            .let { acc -> this.body.bottomUp(acc, visitor) }
            .let { acc -> visitor(acc, this) }
}

typealias TypeVisitor<C> = (acc: C, type: Type) -> C

/**
 * Visit all the types in this [Type] in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> Type.bottomUp(acc: C, visitor: TypeVisitor<C>): C = when (this) {
    is Type.Array ->
        this.elementType.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Type.Struct -> visitor(acc, this)
    is Type.Union -> visitor(acc, this)
    is Type.Enum -> visitor(acc, this)
    is Type.Typedeffed -> visitor(acc, this)
    is Type.Float -> visitor(acc, this)
    is Type.Int -> visitor(acc, this)
    is Type.Fun ->
        this.returnType
            .bottomUp(acc, visitor)
            .let { acc -> params.fold(acc) { acc, param -> param.type.bottomUp(acc, visitor) }}
            .let { acc -> visitor(acc, this) }
    is Type.Ptr ->
        this.pointeeType.bottomUp(acc, visitor)
            .let { acc -> visitor(acc, this) }
    is Type.Void -> visitor(acc, this)
    is Type.VaList -> visitor(acc, this)
}

/**
 * Visit all the types in this [Field] in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> Field.bottomUp(acc: C, visitor: TypeVisitor<C>): C = when (this) {
    is Field.Anonymous -> subfields.fold(acc) { acc, field ->
        field.bottomUp(acc, visitor)
    }
    is Field.Named     -> this.type.bottomUp(acc, visitor)
}

/**
 * Visit all the types in this [UnitDeclaration] in a bottom-up order,
 * accumulating a value of type [C].
 */
fun <C> UnitDeclaration.TypeDeclaration.bottomUp(acc: C, visitor: TypeVisitor<C>) = when (this) {
    is UnitDeclaration.Enum    -> acc
    is UnitDeclaration.Struct  -> fields.fold({ acc }) { fs ->
        fs.fold(acc) { acc, field ->
            field.bottomUp(acc, visitor)
        }
    }
    is UnitDeclaration.Union   -> fields.fold({ acc }) { fs ->
        fs.fold(acc) { acc, field ->
            field.bottomUp(acc, visitor)
        }
    }
    is UnitDeclaration.Typedef -> underlyingType.bottomUp(acc, visitor)
}