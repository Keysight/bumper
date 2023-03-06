package com.riscure.bumper.ast

import arrow.core.Option
import arrow.core.none
import arrow.core.some


data class FloatConstant(val hex: Boolean, val intPart: String, val fracPart: String, val exp: String)

sealed interface Constant {
    data class CInt  (val value: Int, val kind: IKind): Constant
    data class CFloat(val value: FloatConstant, val kind: FKind): Constant
    data class CStr  (val value: String): Constant
    // data class CWStr (val value: List<String>): Constant()
    // data class CEnum (val value: Pair<Ref, Int>): Constant()
}

enum class Assoc { LR, RL, NA }
data class Prec(val prec: Int, val assoc: Assoc) {
    companion object {
        fun lr(prec: Int) = Prec(prec, Assoc.LR)
        fun rl(prec: Int) = Prec(prec, Assoc.RL)
        fun na(prec: Int) = Prec(prec, Assoc.NA)
    }
}

sealed class UnaryOp(val prec: Prec) {
    data class ODot(val member: Ident): UnaryOp(Prec.lr(16))
    data class OArrow(val member: Ident): UnaryOp(Prec.lr(16))
    object OPostIncr: UnaryOp(Prec.lr(16))
    object OPostDecr: UnaryOp(Prec.lr(16))

    object OMinus: UnaryOp(Prec.rl(15))
    object OPlus: UnaryOp(Prec.rl(15))
    object ONot: UnaryOp(Prec.rl(15))
    object OLogNot: UnaryOp(Prec.rl(15))
    object ODeref: UnaryOp(Prec.rl(15))
    object OAddrOf: UnaryOp(Prec.rl(15))
    object OPreIncr: UnaryOp(Prec.rl(15))
    object OPreDecr: UnaryOp(Prec.rl(15))
}

enum class BinaryOp(val prec: Prec) {
    OIndex(Prec.lr(16)), // "a[i]"

    OMul(Prec.lr(13)), // binary "*"
    ODiv(Prec.lr(13)), // "/"
    OMod(Prec.lr(13)), // "%"

    OAdd(Prec.lr(12)), // binary "+"
    OSub(Prec.lr(12)), // binary "+"

    OShl(Prec.lr(11)), // "<<"
    OShr(Prec.lr(11)), // ">>"

    OLt(Prec.lr(10)),  // "<"
    OGt(Prec.lr(10)),  // ">"
    OLe(Prec.lr(10)),  // "<="
    OGe(Prec.lr(10)),  // ">="

    OEq(Prec.lr(9)),  // "=="
    ONe(Prec.lr(9)),  // "!="

    OAnd(Prec.lr(8)), // "&"

    OXor(Prec.lr(7)), // "^"

    OOr(Prec.lr(6)),  // "|"

    OLogAnd(Prec.lr(5)), // "&&"
    OLogOr(Prec.lr(4)),  // "||"

    OAssign(Prec.rl(2)), // "="
    OAddAssign(Prec.rl(2)), // "+="
    OSubAssign(Prec.rl(2)), // "-="
    OMulAssign(Prec.rl(2)), // "*="
    ODivAssign(Prec.rl(2)), // "/="
    OModAssign(Prec.rl(2)), // "%="
    OAndAssign(Prec.rl(2)), // "&="
    OOrAssign(Prec.rl(2)), // "|="
    OXorAssign(Prec.rl(2)), // "^="
    OShlAssign(Prec.rl(2)), // "<<="
    OShrAssign(Prec.rl(2)), // ">>="

    OComma(Prec.lr(1))  // ","
}

/* expression */
sealed interface Exp {
    val etype: Type
    val prec: Prec

    // in order of precedence

    data class Const(
        val constant: Constant,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.na(16)
    }

    data class Var(
        val name: Ident,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.na(16)
    }

    data class Sizeof(
        val type: Type,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(15)
    }

    data class Alignof(
        val type : Type,
        override val etype : Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(15)
    }

    // compound literals
    // - (Point) { .x = 1; .y = 2; }
    data class Compound(
        val initializer: Initializer.Compound,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(14)
    }

    data class Cast(
        val toType: Type,
        val exp: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(14)
    }

    data class UnOp(
        val op: UnaryOp,
        val exp: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = op.prec
    }

    data class BinOp(
        val op: BinaryOp,
        val left: Exp,
        val right: Exp,
        val atType: Type,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = op.prec
    }

    data class Conditional(
        val condition: Exp,
        val thenBranch: Exp,
        val elseBranch: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(3)
    }

    data class Call(
        val funRef: Exp,
        val args: List<Exp>,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.lr(16)
    }

    companion object {
        @JvmStatic
        fun constant(i: Int, kind: IKind) = Const(Constant.CInt(i, kind), Type.Int(kind))
        @JvmStatic
        fun not(exp: Exp) = UnOp(UnaryOp.OLogNot, exp, Type.bool)
        @JvmStatic
        fun dot(exp: Exp, field: Field) = UnOp(UnaryOp.ODot(field.name), exp, field.type)

        @JvmStatic
        fun arrow(exp: Exp, field: Field) = UnOp(UnaryOp.OArrow(field.name), exp, field.type)

        @JvmStatic
        fun addrOf(exp: Exp, ptrKind: IKind) = UnOp(UnaryOp.OAddrOf, exp, Type.Int(ptrKind))

        @JvmStatic
        fun sizeOf(typ: Type, sizeKind: IKind) = Sizeof(typ, Type.Int(sizeKind))

        @JvmStatic
        fun assign(lhs: Exp, rhs: Exp) = BinOp(BinaryOp.OAssign, lhs, rhs, lhs.etype, lhs.etype)

        @JvmStatic
        fun op(op: BinaryOp, lhs: Exp, rhs: Exp) = BinOp(op, lhs, rhs, lhs.etype, lhs.etype)

        @JvmStatic
        fun call(function: Exp, vararg args: Exp) = when (val typ = function.etype) {
            is Type.Fun -> Call(function, args.toList(), typ.returnType)
            else -> throw RuntimeException("Expected expression with function type, got $typ")
        }

        @JvmStatic
        fun struct(struct: TypeRef, initializers: Map<Ident, Initializer>) =
            Compound(Initializer.InitStruct(struct, initializers), Type.Struct(struct))

        @JvmStatic
        fun union(union: TypeRef, initializers: Map<Ident, Initializer>) =
            Compound(Initializer.InitUnion(union, initializers), Type.Struct(union))
    }
}

sealed interface Initializer {
    data class InitSingle(val exp: Exp): Initializer

    sealed interface Compound: Initializer
    data class InitArray(val exps: List<Exp>): Compound
    data class InitStruct(val struct: TypeRef, val initializers: Map<Ident, Initializer>): Compound
    data class InitUnion(val union: TypeRef, val initializers: Map<Ident, Initializer>): Compound
}

// data class AsmOperand(val wut: Option<String>, val wat: Exp)
// typealias AsmOperands = List<AsmOperand>

/* Statements */
sealed interface Stmt {
    // variable declarations:
    // - Point x;
    // - Point x = { .x = 1; .x = 2; };
    data class Decl(
        val storage: Storage = Storage.Default,
        val ident: Ident,
        val type: Type,
        val init: Option<Initializer> = none()
    ): Stmt
    data class Block(val stmts: List<Stmt>): Stmt
    data class Return(val value: Option<Exp> = none()): Stmt
    object Skip: Stmt
    data class Do(val todo: Exp): Stmt
    data class Seq(val first: Stmt, val second: Stmt): Stmt
    data class Conditional(val condition: Exp, val thenBranch: Stmt, val elseBranch: Stmt): Stmt
    data class While(val condition: Exp, val body: Stmt): Stmt
    data class DoWhile(val body: Stmt, val condition: Exp): Stmt
    data class For(val before: Stmt, val condition: Exp, val after: Stmt, val body: Stmt): Stmt
    object Break: Stmt
    object Continue: Stmt
    data class Switch(val scrutinee: Exp, val body: Stmt): Stmt
    data class Labeled(val label: StmtLabel, val stmt: Stmt): Stmt
    data class Goto(val label: StmtLabel.Label): Stmt
    // data class Asm(val instr: String, val operands: AsmOperands) // TODO check missing properties

    companion object {

        @JvmStatic
        fun exp(exp: Exp) = Do(exp)
        @JvmStatic
        fun decl(name: Ident, type: Type, init: Initializer) = Decl(ident = name, type = type, init = init.some())
        @JvmStatic
        fun decl(name: Ident, type: Type) = Decl(ident = name, type = type)
        @JvmStatic
        fun ret(value: Exp) = Return(value.some())
        @JvmStatic
        fun ret() = Return(none())
        @JvmStatic
        fun seq(vararg stmts: Stmt) = seq(stmts.toList())
        @JvmStatic
        fun seq(stmts: List<Stmt>) = stmts.foldRight(Skip as Stmt) { stmt, acc -> Seq(stmt, acc) }
        @JvmStatic
        fun cond(condition: Exp, then: Stmt) = cond(condition, then, Skip)
        @JvmStatic
        fun cond(condition: Exp, then: Stmt, els: Stmt) = Conditional(condition, then, els)
        @JvmField
        val skip = Stmt.Skip
        @JvmStatic
        fun cswitch(scrutinee: Exp, cases: List<Pair<Int,Stmt>>) =
            Switch(scrutinee, seq(cases.map { (c, body) -> ccase(c, body) }))
        @JvmStatic
        fun ccase(c: Int, body: Stmt) = seq(
            Labeled(StmtLabel.Case(Exp.constant(c, IKind.IInt), c), body),
            Break
        )
    }
}

sealed class StmtLabel {
    data class Label(val label: String) : StmtLabel() {
        override fun toString() = "$label"
    }
    data class Case(val case: Exp, val num: Int): StmtLabel() {
        override fun toString() = "case $num"
    }
    object DefaultCase: StmtLabel() {
        override fun toString() = "default"
    }
}