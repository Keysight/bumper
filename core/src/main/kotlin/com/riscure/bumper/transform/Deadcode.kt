package com.riscure.bumper.transform

import arrow.core.*
import com.riscure.bumper.analyses.LinkAnalysis
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.parser.UnitState

/**
 * Removes "dead declarations" from a link target, represented by a set of units.
 * Anything that [alive] depends on is kept alive.
 */
fun <E,S> eliminateDeadDeclarations(
    units: Collection<UnitState<E, S>>,
    alive: Set<Symbol>
): Either<Throwable, Collection<TranslationUnit<E, S>>> = Either.catch {
    val dependencies = LinkAnalysis
        .crossUnitDependencyGraph(units)
        .getOrHandle { throw Throwable(it) }

    // we start with the given set of symbols to keep
    val worklist     = alive.toMutableList()
    val reachable    = mutableSetOf<Symbol>()

    // then we recursively add dependencies,
    // monotonically growing the set of reachable nodes in the dependency graph
    while (worklist.isNotEmpty()) {
        val focus = worklist.removeAt(0)

        if (focus in reachable) continue // already analyzed
        else reachable.add(focus)

        worklist.addAll(dependencies.getOrDefault(focus, listOf()))
    }

    // Now we have to filter the ASTS
    val asts = units.map { it.ast.getOrHandle { throw Throwable(it) } }

    asts.map { unit ->
        unit.copy(
            declarations = unit
                .declarations
                .filter { decl -> decl.mkSymbol(unit.tuid) in reachable }
        )
    }
}
