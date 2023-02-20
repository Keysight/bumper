package com.riscure.bumper.ast

import arrow.core.Option
import arrow.core.none


data class FloatConstant(val hex: Boolean, val intPart: String, val fracPart: String, val exp: String)

sealed interface Constant {
    data class CInt  (val value: Int, val kind: IKind): Constant
    data class CFloat(val value: FloatConstant, val kind: FKind): Constant
    data class CStr  (val value: String): Constant
    // data class CWStr (val value: List<String>): Constant()
    // data class CEnum (val value: Pair<Ref, Int>): Constant()
}

typealias FieldInitializer = Pair<Ident, Initializer>

/* expression */
sealed class Exp {
    data class Const(val constant: Constant): Exp()
    data class Compound(val type: Type, val initializer: Initializer): Exp()
    data class Sizeof(val type: Type): Exp()
    data class Alignof(val type : Type): Exp()
    data class Var(val name: Ident): Exp()
    //data class EUnop(val op: UnOp, val exp: Exp): Exp()
    //data class EBinop(val op: BinOp, val left: Exp, val right: Exp, val atType: Type): Exp()
    data class Conditional(val condition: Exp, val thenBranch: Exp, val elseBranch: Exp): Exp()
    data class Cast(val toType: Type, val exp: Exp): Exp()
    data class Call(val funRef: Exp, val args: List<Exp>): Exp() {
        constructor(funRef: Exp, vararg args: Exp): this(funRef, args.toList())
    }
}

sealed class Initializer {
    data class InitSingle(val exp: Exp): Initializer()
    data class InitArray(val exps: List<Exp>): Initializer()
    data class InitStruct(val struct: Ident, val initializers: List<FieldInitializer>): Initializer()
    data class InitUnion(val union: Ident, val fieldInitializer: FieldInitializer): Initializer()
}

// data class AsmOperand(val wut: Option<String>, val wat: Exp)
// typealias AsmOperands = List<AsmOperand>

/* Statements */
sealed class Stmt {
    data class Decl(val storage: Storage, val name: Ident, val type: Type, val init: Option<Initializer> = none()): Stmt()
    data class Block(val stmts: List<Stmt>): Stmt()
    data class Return(val value: Option<Exp> = none()): Stmt()
    object Skip: Stmt()
    data class Do(val todo: Exp): Stmt()
    data class Seq(val first: Stmt, val snd: Stmt): Stmt()
    data class Conditional(val condition: Exp, val thenBranch: Stmt, val elseBranch: Stmt): Stmt()
    data class While(val condition: Exp, val body: Stmt): Stmt()
    data class DoWhile(val body: Stmt, val condition: Exp): Stmt()
    data class For(val before: Stmt, val condition: Exp, val after: Stmt, val body: Stmt): Stmt()
    class Break: Stmt()
    class Continue: Stmt()
    data class Switch(val scrutinee: Exp, val body: Stmt): Stmt()
    data class Labeled(val label: StmtLabel, val stmt: Stmt): Stmt()
    data class Goto(val label: StmtLabel.Label): Stmt()
    // data class Asm(val instr: String, val operands: AsmOperands) // TODO check missing properties
}

sealed class StmtLabel {
    data class Label(val label: String) : StmtLabel()
    data class Case(val case: Exp, val num: Int): StmtLabel()
    object DefaultCase: StmtLabel()
}