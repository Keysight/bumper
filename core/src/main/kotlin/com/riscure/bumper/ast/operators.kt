package com.riscure.bumper.ast

/** Associativity of an operator */
enum class Assoc {
    /** Left to right */
    LR,
    /** Right to left */
    RL,
    /** Non-associative */
    NA
}

/** Global precedence of an operator */
data class Prec(val prec: Int, val assoc: Assoc) {
    companion object {
        fun lr(prec: Int) = Prec(prec, Assoc.LR)
        fun rl(prec: Int) = Prec(prec, Assoc.RL)
        fun na(prec: Int) = Prec(prec, Assoc.NA)
    }
}

/** Unary operators of C */
sealed class UnaryOp(val prec: Prec) {
    data class OpDot(val member: Ident): UnaryOp(Prec.lr(16))
    data class OpArrow(val member: Ident): UnaryOp(Prec.lr(16))
    object OpPostIncr: UnaryOp(Prec.lr(16))
    object OpPostDecr: UnaryOp(Prec.lr(16))

    object OpMinus: UnaryOp(Prec.rl(15))
    object OpPlus: UnaryOp(Prec.rl(15))
    object OpNot: UnaryOp(Prec.rl(15))
    object OpLogNot: UnaryOp(Prec.rl(15))
    object OpDeref: UnaryOp(Prec.rl(15))
    object OpAddrOf: UnaryOp(Prec.rl(15))
    object OpPreIncr: UnaryOp(Prec.rl(15))
    object OpPreDecr: UnaryOp(Prec.rl(15))
}

/** Binary operators of C */
enum class BinaryOp(val prec: Prec) {
    OpIndex(Prec.lr(16)), // "a[i]"

    OpMul(Prec.lr(13)), // binary "*"
    OpDiv(Prec.lr(13)), // "/"
    OpMod(Prec.lr(13)), // "%"

    OpAdd(Prec.lr(12)), // binary "+"
    OpSub(Prec.lr(12)), // binary "+"

    OpShl(Prec.lr(11)), // "<<"
    OpShr(Prec.lr(11)), // ">>"

    OpLt(Prec.lr(10)),  // "<"
    OpGt(Prec.lr(10)),  // ">"
    OpLe(Prec.lr(10)),  // "<="
    OpGe(Prec.lr(10)),  // ">="

    OpEq(Prec.lr(9)),  // "=="
    OpNe(Prec.lr(9)),  // "!="

    OpAnd(Prec.lr(8)), // "&"

    OpXor(Prec.lr(7)), // "^"

    OpOr(Prec.lr(6)),  // "|"

    OpLogAnd(Prec.lr(5)), // "&&"
    OpLogOr(Prec.lr(4)),  // "||"

    OpAssign(Prec.rl(2)), // "="
    OpAddAssign(Prec.rl(2)), // "+="
    OpSubAssign(Prec.rl(2)), // "-="
    OpMulAssign(Prec.rl(2)), // "*="
    OpDivAssign(Prec.rl(2)), // "/="
    OpModAssign(Prec.rl(2)), // "%="
    OpAndAssign(Prec.rl(2)), // "&="
    OpOrAssign(Prec.rl(2)), // "|="
    OpXorAssign(Prec.rl(2)), // "^="
    OpShlAssign(Prec.rl(2)), // "<<="
    OpShrAssign(Prec.rl(2)), // ">>="

    OpComma(Prec.lr(1))  // ","
}