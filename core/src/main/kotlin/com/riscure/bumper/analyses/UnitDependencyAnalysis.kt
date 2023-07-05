package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*

typealias Result<T> = Either<String, Set<T>>

fun <T> nil(): Result<T> = setOf<T>().right()
fun <T> Result<T>.union(that: Result<T>) = flatMap { left -> that.map { right -> left + right }}

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

    fun ofUnit(unit: TranslationUnit<Exp, Stmt>): Either<String, DependencyGraph> =
        unit.declarations
            .map { d -> ofDecl(d).map { deps -> Pair(d.mkSymbol(unit.tuid), deps) }}
            .sequence()
            .map { entries ->
                // We may have entries with clashing keys,
                // due to different declarations/definitions belonging to the same symbol.
                // Hence, we need to take care to merge entries, rather than bluntly using .toMap()
                DependencyGraph.union(entries.map { (k, deps) ->
                    DependencyGraph.build(mapOf(k to deps.map { it.symbol(unit.tuid) }.toSet()))
                })
            }

    fun ofExp(exp: Exp): Result<TLID>
    fun ofStmt(stmt: Stmt): Result<TLID>

    fun ofDecl(decl: UnitDeclaration<Exp, Stmt>): Result<TLID> = when (decl) {
        is UnitDeclaration.Var       ->
            decl.rhs
                .map { ofExp(it) }
                .getOrElse { nil() }
                .union(ofType(decl.type))
        is UnitDeclaration.Compound ->
            decl.fields
                .map(::ofFields)
                .getOrElse { nil() }
        is UnitDeclaration.Enum      -> nil()
        is UnitDeclaration.Fun       ->
            decl.body
                .map { ofStmt(it) }
                .getOrElse { nil() }
                .union(ofType(decl.returnType))
                .union(ofParams(decl.params))
        is UnitDeclaration.Typedef   ->
            ofType(decl.underlyingType)
    }

    fun ofFields(fields: FieldDecls): Result<TLID> =
        fields
            .map { f: Field ->
                when (f) {
                    is Field.Anonymous -> ofFields(f.subfields)
                    is Field.Named -> ofType(f.type)

                }
            }
            .sequence()
            .map { it.flatten().toSet() }

    fun ofType(type: Type): Result<TLID> = when (type) {
        is Type.Fun       ->
            ofType(type.returnType)
                .union(ofParams(type.params))
        is Type.Array     -> ofType(type.elementType)
        is Type.Ptr       -> ofType(type.pointeeType)
        is Type.Typedeffed-> setOf(type.ref).right()
        is Type.Struct    -> setOf(type.ref).right()
        is Type.Union     -> setOf(type.ref).right()
        is Type.Enum      -> setOf(type.ref).right()
        is Type.Int       -> nil()
        is Type.Float     -> nil()
        is Type.Void      -> nil()
//        is Type.Atomic    -> ofType(type.elementType)
//        is Type.Complex   -> nil()
        is Type.VaList    -> nil()
    }

    fun ofExps(exps: List<Exp>) = exps
        .map { ofExp(it) }
        .sequence()
        .map { it.flatten().toSet() }

    private fun ofParams(params: List<Param>): Result<TLID> =
        params
            .map { ofType(it.type) }
            .sequence()
            .map { it.flatten().toSet() }
}
