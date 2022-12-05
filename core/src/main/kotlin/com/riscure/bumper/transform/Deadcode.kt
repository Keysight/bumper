package com.riscure.bumper.transform

import arrow.core.*
import com.riscure.bumper.analyses.DependencyGraph
import com.riscure.bumper.analyses.LinkAnalysis
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.parser.UnitState

/**
 * Removes "dead declarations" from a link target, represented by a set of units.
 * Anything that [alive] depends on is kept alive.
 */
object Deadcode {

    @JvmStatic
    fun <E, S> eliminate(dependencies: DependencyGraph, units: Collection<UnitState<E, S>>, alive: Set<Symbol>)
    : Either<Throwable, Collection<TranslationUnit<E, S>>> = Either.catch {
        val reachable = dependencies.reachable(alive)

        units
            .map { unit ->
                unit.ast.copy(
                    declarations = unit.ast.declarations
                        .filter { decl -> decl.mkSymbol(unit.tuid) in reachable }
                )
            }
    }

    @JvmStatic
    fun <E, S> eliminate(units: Collection<UnitState<E, S>>, alive: Set<Symbol>)
    : Either<String, Collection<TranslationUnit<E, S>>> = Either.catch({ e -> e.message!! }) {
        val asts = units.map { it.ast }

        val linkDependencies = LinkAnalysis
            .linkGraph(asts)
            .getOrHandle { throw Throwable(it) }
            .externalDependencyGraph

        val internalDependencies = units
            .map { unit -> unit.dependencies }
            .sequence()
            .getOrHandle { throw Throwable(it) }
            .let { DependencyGraph.union(it) }

        val dependencies = linkDependencies.union(internalDependencies)

        eliminate(dependencies, units, alive).getOrHandle { throw it }
    }
}