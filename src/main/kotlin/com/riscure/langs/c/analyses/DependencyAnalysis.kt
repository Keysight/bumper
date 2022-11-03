package com.riscure.langs.c.analyses

import arrow.core.*
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.parser.clang.ICursorParser

typealias Result = Either<String, Set<TLID>>

fun Result.union(that: Result) = flatMap { left -> that.map { right -> left + right }}

/**
 * Implements a dependency analysis for C programs.
 */
interface DependencyAnalysis<Exp,Stmt>: ICursorParser {
    fun ofExp(exp: Exp): Result
    fun ofStmt(stmt: Stmt): Result

    fun ofDecl(decl: Declaration<Exp,Stmt>): Result = when (decl) {
        is Declaration.Var       ->
            decl.rhs
                .map { ofExp(it) }
                // if no rhs is given, no dependencies
                .getOrElse { setOf<TLID>().right() }
                .union(ofType(decl.type))
        is Declaration.Composite                                       ->
            decl.fields
                .map { fields ->
                    fields
                        .map { ofType(it.type) }
                        .sequence()
                        .map { it.flatten().toSet() }
                }
                // if no fields are defined, no dependencies
                .getOrElse { setOf<TLID>().right() }
        is Declaration.Enum ->
            setOf<TLID>().right()
        is Declaration.Fun                                             ->
            decl.body
                .map { ofStmt(it) }
                // if no body, no dependencies
                .getOrElse { setOf<TLID>().right() }
                .union(ofType(decl.returnType))
                .union(ofParams(decl.params))
        is Declaration.Typedef   ->
            ofType(decl.underlyingType)
    }

    fun ofType(type: Type): Result = when (type) {
        is Type.Fun               ->
            ofType(type.returnType)
                .union(ofParams(type.params))
        is Type.Array             -> ofType(type.elementType)
        is Type.Ptr               -> ofType(type.pointeeType)
        is Type.Typedeffed        -> type.ref.reffed.tlid.toList().toSet().right()
        is Type.Struct            -> type.ref.reffed.tlid.toList().toSet().right()
        is Type.Union             -> type.ref.reffed.tlid.toList().toSet().right()
        is Type.Int               -> setOf<TLID>().right()
        is Type.Enum              -> setOf<TLID>().right()
        is Type.Float             -> setOf<TLID>().right()
        is Type.Void              -> setOf<TLID>().right()
        is Type.Atomic            -> ofType(type.elementType)
        is Type.Complex           -> setOf<TLID>().right()
        is Type.InlineDeclaration -> ofDecl(type.declaration)
    }

    private fun ofParams(params: List<Param>): Result =
        params
            .map { ofType(it.type) }
            .sequence()
            .map { it.flatten().toSet() }
}