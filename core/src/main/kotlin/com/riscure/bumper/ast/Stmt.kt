package com.riscure.bumper.ast

import arrow.core.Option
import arrow.core.none
import arrow.core.some

/** C's statements */
sealed interface Stmt {

    // variable declarations:
    // - Point x;
    // - Point x = { .x = 1; .x = 2; };
    data class Decl(
        val storage: Storage = Storage.Default,
        val ident: Ident,
        val type: Type,
        val init: Option<Initializer> = none()
    ): Stmt {
        val ref get() = Exp.Var(ident, type)
    }
    data class Block(val stmts: List<Stmt>): Stmt
    data class Return(val value: Option<Exp> = none()): Stmt
    object Skip: Stmt
    data class Do(val todo: Exp): Stmt
    data class Seq(val first: Stmt, val second: Stmt): Stmt
    data class Conditional(val condition: Exp, val thenBranch: Stmt, val elseBranch: Stmt): Stmt
    data class While(val condition: Exp, val body: Stmt): Stmt
    data class DoWhile(val body: Stmt, val condition: Exp): Stmt
    data class For(val before: Stmt, val condition: Exp, val after: Exp, val body: Stmt): Stmt
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
        fun decl(name: Ident, type: Type, init: Exp) =
            Decl(ident = name, type = type, init = Initializer.InitSingle(init).some())
        @JvmStatic
        fun decl(name: Ident, type: Type) = Decl(ident = name, type = type)
        @JvmStati
        fun ret(value: Exp) = Return(value.some())
        @JvmStatic
        fun ret() = Return(none())
        @JvmStatic
        fun seq(vararg stmts: Stmt) = seq(stmts.toList())
        @JvmStatic
        fun seq(stmts: List<Stmt>) = stmts
            // we define it in such a way that seq(listOf()) = seq(listOf(seq(seq(skip)))) = Skip
            .foldSeq(Skip as Stmt) { acc, stmt -> Seq(acc, stmt) }
        @JvmStatic
        fun cond(condition: Exp, then: Stmt) = cond(condition, then, Skip)
        @JvmStatic
        fun cond(condition: Exp, then: Stmt, els: Stmt) = Conditional(condition, then, els)
        @JvmField
        val skip = Stmt.Skip
        @JvmStatic
        fun cswitch(scrutinee: Exp, cases: List<Pair<Int, Stmt>>) =
            Switch(scrutinee, seq(cases.map { (c, body) -> ccase(c, body) }))
        @JvmStatic
        fun ccase(c: Int, body: Stmt) = seq(
            Labeled(StmtLabel.Case(Exp.constant(c.toLong(), IKind.IInt), c), body),
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

/**
 * Fold over the statements in the sequence of statements, skipping skip-statements and recursively
 * folding in order nested sequences.
 */
fun <A> List<Stmt>.foldSeq(initial: A, block: (A, Stmt) -> A): A =
    this.fold(initial) { acc, stmt ->
        when (stmt) {
            Stmt.Skip   -> acc
            is Stmt.Seq -> stmt.foldSeq(acc, block)
            else        -> block(acc, stmt)
        }
    }

fun <A> Stmt.Seq.foldSeq(initial: A, block: (A, Stmt) -> A): A =
    listOf(first, second).foldSeq(initial, block)