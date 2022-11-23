package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol

typealias Result = Either<String, Set<Symbol>>

fun Result.union(that: Result) = flatMap { left -> that.map { right -> left + right }}

/**
 * Implements a dependency analysis for C programs:
 * that is, it computes a set of symbols that a given AST element depends on,
 * directly or transitively.
 *
 * This eagerly computes transitive dependencies, making use of caching
 * to avoid recomputing dependencies for the same symbol multiple times.
 * It maintains a stack to detect cycles in the dependency graph.
 */
interface DependencyAnalysis<Exp,Stmt> {
    val nil: Result get() = setOf<Symbol>().right()

    fun ofExp(exp: Exp): Result
    fun ofStmt(stmt: Stmt): Result

    fun ofDecl(decl: Declaration<Exp, Stmt>): Result = when (decl) {
        is Declaration.Var       ->
            decl.rhs
                .map { ofExp(it) }
                // if no rhs is given, no dependencies
                .getOrElse { nil }
                .union(ofType(decl.type))
        is Declaration.Composite ->
            decl.fields
                .map { fields ->
                    fields
                        .map { ofType(it.type) }
                        .sequence()
                        .map { it.flatten().toSet() }
                }
                // if no fields are defined, no dependencies
                .getOrElse { nil }
        is Declaration.Enum      -> nil
        is Declaration.Fun       ->
            decl.body
                .map { ofStmt(it) }
                .getOrElse { nil } // if no body, no dependencies
                .union(ofType(decl.returnType))
                .union(ofParams(decl.params))
        is Declaration.Typedef   ->
            ofType(decl.underlyingType)
    }

    fun ofType(type: Type): Result = TODO() /* when (type) {
        is Type.Fun               ->
            ofType(type.returnType)
                .union(ofParams(type.params))
        is Type.Array             -> ofType(type.elementType)
        is Type.Ptr               -> ofType(type.pointeeType)
        is Type.Typedeffed        -> setOf(type.ref.resolution).right()
        is Type.Struct            -> setOf(type.ref.resolution).right()
        is Type.Union             -> setOf(type.ref.resolution).right()
        is Type.Int               -> nil
        is Type.Enum              -> nil
        is Type.Float             -> nil
        is Type.Void              -> nil
        is Type.Atomic            -> ofType(type.elementType)
        is Type.Complex           -> nil
        is Type.InlineDeclaration -> ofDecl(type.declaration)
    }*/

    private fun ofParams(params: List<Param>): Result =
        params
            .map { ofType(it.type) }
            .sequence()
            .map { it.flatten().toSet() }
}