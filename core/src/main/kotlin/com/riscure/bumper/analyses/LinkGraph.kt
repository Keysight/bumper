package com.riscure.bumper.analyses

import arrow.core.toOption
import com.riscure.bumper.ast.UnitDeclaration
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID

/**
 * A [DeclarationInUnit] is a pair of a translation unit identifier [tuid]
 * and a [proto]type of a value (function or global) in the identified unit.
 */
data class DeclarationInUnit(
    val tuid: TUID,
    val proto: UnitDeclaration.Valuelike<*, *>
) {
    val name: String get() = proto.tlid.name
    val symbol: Symbol get() = Symbol(tuid, proto.tlid)
}

/**
 * A [Link] associates a [declaration] without a definition in one unit
 * with a [definition] of the same symbol in another unit.
 */
data class Link(
    val declaration: DeclarationInUnit,
    val definition : DeclarationInUnit,
)

data class Linking(
    val bound: Set<Link>,
    val unbound: Set<UnitDeclaration.Valuelike<*, *>>
)

/**
 * The linkgraph only represents inter-unit dependencies.
 * Such dependencies come from linking declarations to definitions.
 *
 * It consists of a set [dependencies] of [Link]s for every translation unit U identified by a [TUID].
 * Every [Link] associates a declaration in the unit U, with some declaration in some other known unit V.
 *
 * A linkgraph can be computed from a set of translation units using [LinkAnalysis].
 */
data class LinkGraph(
    val dependencies: Map<TUID, Linking>,
) {
    /**
     * All units whose definitions are linked to in [dependencies].
     */
    val linkedUnits: Set<TUID> get() =
        dependencies
            .values
            .asIterable()
            .flatMapTo(mutableSetOf()) { it.bound.map { it.definition.tuid }}

    /**
     * Get a complete set of used-but-not-defined symbols.
     */
    val unbound: Set<DeclarationInUnit> get() =
        dependencies.entries
            .flatMap { (tuid, linking) ->
                linking.unbound
                    .map { DeclarationInUnit(tuid, it) }
            }
            .toSet()

    /**
     * For every symbol, a set of symbols in other translation unit that it depends on.
     */
    val externalDependencyGraph: DependencyGraph by lazy {
        val result = mutableMapOf<Symbol, MutableSet<Symbol>>()

        for ((_, links) in dependencies) {
            for ((declaration, definition) in links.bound) {
                val from = declaration.symbol

                val deps = result.getOrDefault(from, mutableSetOf())
                result[from] = deps

                deps.add(definition.symbol)
            }
        }

        DependencyGraph(result)
    }

    /** Get the linking data for the give [tuid] */
    operator fun get(tuid: TUID) = dependencies[tuid].toOption()

    /**
     * Get the *external direct dependencies* for the given [symbol]
     */
    operator fun get(symbol: Symbol): Set<Symbol> = externalDependencyGraph
        .dependencies
        .getOrDefault(symbol, setOf())

    companion object {
        /**
         * Construct an [LinkGraph] from a mapping representing [links] between declarations and definitions and
         * a set of [unbound] declarations.
         */
        operator fun invoke(links: List<Link>, unbound: Set<DeclarationInUnit>): LinkGraph {
            val unboundByTUID = unbound.groupBy({ it.tuid }) { it.proto }
            val edgeset = links
                .groupBy { it.declaration.tuid }
                .mapValues { (tuid, bound) ->
                    Linking(bound.toSet(), unboundByTUID.getOrDefault(tuid, listOf()).toSet())
                }

            return LinkGraph(edgeset)
        }
    }
}