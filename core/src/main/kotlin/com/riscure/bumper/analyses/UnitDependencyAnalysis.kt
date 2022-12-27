package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol

typealias Result = Either<String, Set<Symbol>>

fun Result.union(that: Result) = flatMap { left -> that.map { right -> left + right }}

/**
 * Implements a dependency analysis for C translation units.
 *
 * That is, it computes a set of symbols that a given AST element depends on, *directly*.
 * To compute the full set of dependencies, you have to take the reflexive-transitive
 * closure of the computed direct dependencies.
 *
 * By the nature of C, the dependencies are always local to the translation unit.
 * This can be extended to a dependency graph across translation units using the [LinkAnalysis].
 */
interface UnitDependencyAnalysis<Exp, Stmt> {
    val nil: Result get() = setOf<Symbol>().right()

    fun ofUnit(unit: TranslationUnit<Exp, Stmt>): Either<String, DependencyGraph> =
        unit.declarations
            .map { d -> ofDecl(d).map { deps -> Pair(d.mkSymbol(unit.tuid), deps) }}
            .sequence()
            .map { entries ->
                // We may have entries with clashing keys,
                // due to different declarations/definitions belonging to the same symbol.
                // Hence, we need to take care to merge entries, rather than bluntly using .toMap()
                DependencyGraph.union(entries.map { (k, deps) -> DependencyGraph(mapOf(k to deps))})
            }

    fun ofExp(exp: Exp): Result
    fun ofStmt(stmt: Stmt): Result

    fun ofDecl(decl: Declaration<Exp, Stmt>): Result = when (decl) {
        is Declaration.Var       ->
            decl.rhs
                .map { ofExp(it) }
                .getOrElse { nil }
                .union(ofType(decl.type))
        is Declaration.Composite ->
            decl.fields
                .map(::ofFields)
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

    fun ofFields(fields: FieldDecls) =
        fields
            .map { ofType(it.type) }
            .sequence()
            .map { it.flatten().toSet() }

    fun ofType(type: FieldType): Result = when (type) {
        is Type.Fun                ->
            ofType(type.returnType)
                .union(ofParams(type.params))
        is Type.Array              -> ofType(type.elementType)
        is Type.Ptr                -> ofType(type.pointeeType)
        is Type.Typedeffed         -> setOf(type.ref).right()
        is Type.Struct             -> setOf(type.ref).right()
        is Type.Union              -> setOf(type.ref).right()
        is Type.Int                -> nil
        is Type.Enum               -> nil
        is Type.Float              -> nil
        is Type.Void               -> nil
        is Type.Atomic             -> ofType(type.elementType)
        is Type.Complex            -> nil
        is Type.VaList             -> nil
        is FieldType.AnonComposite -> ofFields(type.fields.getOrElse { listOf() })
    }

    private fun ofParams(params: List<Param>): Result =
        params
            .map { ofType(it.type) }
            .sequence()
            .map { it.flatten().toSet() }
}