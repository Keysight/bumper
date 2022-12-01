package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID

typealias LinkGraph<E, S> = Map<TUID, Set<LinkAnalysis.Dependency<E, S>>>

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

    data class Dependency<E, S>(
        val unit: TUID,
        val needs: Declaration.Valuelike<E,S>
    )

    /**
     * Compute the edgeset for each node in the dependency graph
     */
    fun <E, S> dependencyGraph(units: Collection<TranslationUnit<E, S>>): Either<String, LinkGraph<E, S>> {
        // compute for each translation unit its interface
        val interfaces = units.associate { unit -> Pair(unit.tuid, objectInterface(unit)) }

        // Index the exports by TLID.
        // This assumes that we have at most one definition per tlid in the units.
        val exportIndex: Map<TLID, Dependency<E, S>> = interfaces
            .entries
            .flatMap { (tuid, intf) ->
                intf.exports
                    .map { Pair(it.tlid, Dependency(tuid, it)) }
            }
            .toMap()

        // Now try to resolve all imports, obtaining the dependencies
        return Either.catch({ it.message!! }) {
            interfaces
                .mapValues { (_, intf) ->
                    intf.imports
                        .map { import ->
                            exportIndex[import.tlid]
                                .toOption()
                                .getOrElse {
                                    throw Throwable("Could not resolve declaration of ${import.tlid} to definition.")
                                }
                        }
                        .toSet()
                }
        }
    }

    data class UnitInterface<E, S>(
        val imports: Set<Declaration.Valuelike<E,S>>,
        val exports: Set<Declaration.Valuelike<E,S>>,
    )

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