package com.riscure.bumper.analyses

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID

data class DeclarationInUnit<E, S>(
    val unit: TUID,
    val decl: Declaration.Valuelike<E,S>
) {
    val symbol: Symbol get() = decl.mkSymbol(unit)
}

data class Link<E, S>(
    val declaration: DeclarationInUnit<E,S>,
    val definition : DeclarationInUnit<E,S>,
)

data class DependencyGraph(val dependencies: Map<Symbol, Set<Symbol>>) {

    fun union(other: DependencyGraph) = with (graphMonoid) { combine(other) }

    /**
     * Performs a reachability analysis on the dependency graph,
     * returning the set of symbols that are reachale from the roots.
     */
    fun reachable(roots: Set<Symbol>): Set<Symbol> {
        // we start with the given set of symbols to keep
        val worklist     = roots.toMutableList()
        val reachable    = mutableSetOf<Symbol>()

        // then we recursively add dependencies,
        // monotonically growing the set of reachable nodes in the dependency graph
        while (worklist.isNotEmpty()) {
            val focus = worklist.removeAt(0)

            if (focus in reachable) continue // already analyzed
            else reachable.add(focus)

            worklist.addAll(dependencies.getOrDefault(focus, listOf()))
        }

        return reachable
    }

    companion object {
        object graphMonoid: Monoid<DependencyGraph> {
            override fun DependencyGraph.combine(b: DependencyGraph) = DependencyGraph(
                this.dependencies.zip(b.dependencies) { _, l, r -> l + r }
            )

            override fun empty() = DependencyGraph(mapOf())
        }

        @JvmStatic fun empty() = graphMonoid.empty()
        @JvmStatic fun union(graphs: List<DependencyGraph>) = graphMonoid.fold(graphs)

    }
}

data class LinkGraph<E, S>(private val dependencies: Map<TUID, Set<Link<E, S>>>) {

    /**
     * For every symbol, a set of symbols in other translation unit that it depends on.
     */
    val externalDependencyGraph: DependencyGraph by lazy {
        val result = mutableMapOf<Symbol, MutableSet<Symbol>>()

        for ((_, links) in dependencies) {
            for ((declaration, definition) in links) {
                val from = declaration.symbol

                val deps = result.getOrDefault(from, mutableSetOf())
                result[from] = deps

                deps.add(definition.symbol)
            }
        }

        DependencyGraph(result)
    }

    operator fun get(tuid: TUID) = dependencies[tuid].toOption()

    /**
     * Get the *external direct dependencies* for the given [symbol]
     */
    operator fun get(symbol: Symbol): Set<Symbol> = externalDependencyGraph
        .dependencies
        .getOrDefault(symbol, setOf())
}

/**
 * Implements a dependency analysis *between* translation units
 * that together make a link target.
 *
 * For each unit, we compute an object interface with exports (defined symbols)
 * and imports (declared but not defined symbols).
 *
 * We then cross-reference the imports of each unit against the exports
 * from other units, obtaining a dependency graph.
 */
object LinkAnalysis {

    /**
     * Compute the edge-set for each node in the link graph
     */
    @JvmStatic
    fun <E, S> linkGraph(units: Collection<TranslationUnit<E, S>>): Either<String, LinkGraph<E, S>> {
        // compute for each translation unit its interface
        val interfaces = units.associate { unit -> Pair(unit.tuid, objectInterface(unit)) }

        // Index the exports by TLID.
        // This assumes that we have at most one definition per tlid in the units.
        val exportIndex: Map<TLID, DeclarationInUnit<E, S>> = interfaces
            .entries
            .flatMap { (tuid, intf) ->
                intf.exports
                    .map { Pair(it.tlid, DeclarationInUnit(tuid, it)) }
            }
            .toMap()

        // Now try to resolve all imports, obtaining links from declarations to definitions.
        return Either.catch({ it.message!! }) {
            interfaces
                .mapValues { (tuid, intf) ->
                    intf.imports
                        .flatMap { import ->
                            // we try to resolve the import.
                            // If it fails, then we don't have a definition
                            // within the translation unit, and we don't incur a link dependency.
                            exportIndex[import.tlid]
                                .toOption()
                                .map { Link(DeclarationInUnit(tuid, import), it) }
                                .toList()
                        }
                        .toSet()
                }
                .let { LinkGraph(it) }
        }
    }

    data class UnitInterface<E, S>(
        val imports: Set<Declaration.Valuelike<E,S>>,
        val exports: Set<Declaration.Valuelike<E,S>>,
    )

    /**
     * Compute the set of imports and exports of a translation unit.
     */
    @JvmStatic
    fun <E, S> objectInterface(unit: TranslationUnit<E, S>): UnitInterface<E, S> {
        // compute the list of declarations that are visible
        // across linked units
        val visible = unit
            .valuelikeDeclarations
            .filter { decl -> decl.storage != Storage.Static }

        val exports = visible.filter { it.isDefinition }
        val exportIndex = exports.map { it.tlid } .toSet()
        val imports = visible
            .filter { decl -> !exportIndex.contains(decl.tlid) }

        return UnitInterface(imports.toSet(), exports.toSet())
    }
}