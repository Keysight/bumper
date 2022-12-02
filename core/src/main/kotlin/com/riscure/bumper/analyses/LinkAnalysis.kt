package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.UnitState

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

data class LinkGraph<E, S>(private val dependencies: Map<TUID, Set<Link<E, S>>>) {

    /**
     * For every symbol, a set of symbols in other translation unit that it depends on.
     */
    private val externalDependencyGraph: DependencyGraph by lazy {
        val result = mutableMapOf<Symbol, MutableSet<Symbol>>()

        for ((tuid, links) in dependencies) {
            for ((declaration, definition) in links) {
                val from = declaration.symbol

                val deps = result.getOrDefault(from, mutableSetOf())
                result[from] = deps

                deps.add(definition.symbol)
            }
        }

        result
    }

    operator fun get(tuid: TUID) = dependencies[tuid].toOption()

    /**
     * Get the *external direct dependencies* for the given [symbol]
     */
    operator fun get(symbol: Symbol): Set<Symbol> = externalDependencyGraph.getOrDefault(symbol, setOf())
}

typealias DependencyGraph = Map<Symbol, Set<Symbol>>

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
     * Compute the edgeset for each node in the dependency graph
     */
    fun <E, S> dependencyGraph(units: Collection<TranslationUnit<E, S>>): Either<String, LinkGraph<E, S>> {
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
                        .map { import ->
                            val resolution = exportIndex[import.tlid]
                                .toOption()
                                .getOrElse {
                                    throw Throwable("Could not resolve declaration of ${import.tlid} to definition.")
                                }

                            Link(DeclarationInUnit(tuid ,import), resolution)
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

    /**
     * Compute the cross-unit *direct* dependencies of every symbol in the given [units],
     * combining the [UnitDependencyAnalysis] with the [LinkGraph].
     * This powers, for example, the dead-code elimination.
     */
    fun <E,S> crossUnitDependencyGraph(units: Collection<UnitState<E, S>>): Either<Throwable,DependencyGraph> =
        Either.catch {
            val asts  = units.map { it.ast.getOrHandle { throw Throwable(it) }}
            val links = dependencyGraph(asts).getOrHandle { throw Throwable(it) }

            units
                .flatMap { unit ->
                    val ast = unit.ast.getOrHandle { throw Throwable(it) }
                    val pairs: List<Pair<Symbol, Set<Symbol>>> = ast
                        .declarations
                        .map { decl ->
                            val sym = decl.mkSymbol(ast.tuid)

                            val localdeps: Set<Symbol> = unit
                                .dependencies
                                .ofDecl(decl).getOrHandle { throw Throwable(it) }

                            val externaldeps = links.get(sym)

                            Pair(sym, localdeps + externaldeps)
                        }

                    pairs
                }
                .toMap()
        }

}