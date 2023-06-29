package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID

/**
 * [LinkAnalysis] implements a dependency analysis *between* translation units.
 *
 * For each unit, we compute an object interface with exports (defined symbols)
 * and imports (declared but not defined symbols).
 *
 * We then cross-reference the imports of each unit against the exports
 * from other units, obtaining a [LinkGraph].
 */
object LinkAnalysis {

    /**
     * Compute the [LinkGraph] for a set of translation [units],
     * by cross-referencing the imports with the combined exports of all the units,
     * as well as the set of symbols that have no matching definition in the set of [units].
     *
     * This assumes that there are no duplicate definitions in the set of [units].
     * In other words: it is a well typed link target.
     *
     * @return a [LinkGraph] with the linked symbols and
     *         the set of prototypes for which no definition was found.
     */
    @JvmStatic
    operator fun <E, S> invoke(units: Collection<TranslationUnit<E, S>>): LinkGraph {
        // compute for each translation unit its interface
        val interfaces = units.associate { unit -> Pair(unit.tuid, objectInterface(unit)) }

        // Index the exports by TLID.
        // This assumes that we have at most one definition per tlid in the units.
        val exportIndex: Map<TLID, DeclarationInUnit> = interfaces
            .entries
            .flatMap { (tuid, intf) ->
                intf.exports
                    .map { Pair(it.tlid, DeclarationInUnit(tuid, it)) }
            }
            .toMap()

        // Now try to resolve all imports, obtaining links from declarations to definitions.
        val edges: Map<TUID, Linking> = interfaces
            .mapValues { (tuid, intf) ->
                // We keep track of symbols that are missing from the export index.
                val missing = mutableSetOf<UnitDeclaration.Valuelike<*,*>>()

                val links = intf
                    .imports
                    .flatMap { import ->
                        // we try to resolve the import to a matching export from the index.
                        val link: Option<Link> = exportIndex[import.tlid]
                            .toOption()
                            .map { Link(DeclarationInUnit(tuid, import), it) }

                        when (link) {
                            is Some -> listOf(link.value)
                            is None -> {
                                // If resolution fails then we don't have a definition
                                // within the set of translation units, and we don't incur a link dependency.
                                // We record the missing dependency
                                missing.add(import)
                                listOf()
                            }
                        }
                    }
                    .toSet()

                Linking(links, missing)
            }

        return LinkGraph(edges)
    }

    /**
     * A [UnitInterface] is set of imports (declarations without definitions in the unit)
     * together with a set of exports (defined and visible/non-static symbols in the unit).
     */
    data class UnitInterface<E, S>(
        val imports: Set<UnitDeclaration.Valuelike<E,S>>,
        val exports: Set<UnitDeclaration.Valuelike<E,S>>,
    )

    /**
     * Compute the [UnitInterface] of a translation [unit].
     */
    @JvmStatic
    fun <E, S> objectInterface(unit: TranslationUnit<E, S>): UnitInterface<E, S> {
        // compute the list of declarations that are visible
        // across linked units
        val exports = unit
            .valuelikeDefinitions
            .filter { decl -> decl.storage != Storage.Static }
        val exportIndex = exports.map { it.tlid } .toSet()

        val imports = unit
            .valuelikeDeclarations
            .filter { decl -> decl.storage != Storage.Static }
            .filter { decl -> !exportIndex.contains(decl.tlid) }

        return UnitInterface(imports.toSet(), exports.toSet())
    }
}