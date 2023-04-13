package com.riscure.bumper.analyses

import arrow.core.*
import com.riscure.bumper.Frontend
import com.riscure.bumper.ast.EntityKind
import com.riscure.bumper.ast.UnitDeclaration
import com.riscure.bumper.index.Index
import com.riscure.bumper.parser.ParseError
import com.riscure.bumper.parser.Parser
import com.riscure.dobby.clang.CompilationDb

/**
 * This analysis tries to resolve missing symbols from a target
 * using an index of a larger set of translation units,
 * thus computing a "closure" of a set of units.
 *
 * The analysis only proceeds when resolution in the index is unambiguous,
 * and reports ambiguous choices.
 *
 * The analysis is slightly complicated, because it combines high-level information
 * (e.g., the [index]) and low-level information about individual translation units.
 * To get the latter, it may need to parse translation units mentioned in the index
 * along the way. Hence, the index should be 'up-to-date' w.r.t. what exists on disk.
 */
class UnitClosureAnalysis(
    val index: Index,
    val cdb: CompilationDb,
    val frontend: Frontend<*, *, *>
) {
    data class Unresolved(val decl: DeclarationInUnit, val resolutions: Set<Index.Entry>)

    fun closureOf(graph: LinkGraph): Either<ParseError, LinkGraph> = try {
        closureOfOrThrow(graph).right()
    } catch (err: ParseError) { err.left() }

    /**
     * Parse the unit in which [definition] resides, and find all external,
     * undefined value dependencies of that definition that need to be resolved by the linker.
     */
    private fun undefinedDependenciesOfOrThrow(definition: DeclarationInUnit): Collection<DeclarationInUnit> {
        // TODO we should probably avoid parsing the same unit more than once.
        // TODO fix for linking globals?
        // so we should maybe add a local cache?
        val state = frontend
            .process(cdb, definition.tuid.main)
            .getOrHandle { throw it }

        val depGraph = state // todo only analyse dependencies of [definition]
            .dependencies
            .getOrHandle { reason ->
                throw ParseError.AnalysisFailed(
                "Failed to compute dependency graph for unit ${definition.tuid.main}: $reason"
                )
            }

        // collect the dependencies that are
        // a. reachable from [definition]
        // b. functions
        // c. without a definition in [state.ast]
        // In other words: the dependencies that need to be resolved by the linker.
        return depGraph
            .reachable(setOf(definition.symbol))
            .nodes
            .filter { it.kind == EntityKind.Fun || it.kind == EntityKind.Var }
            .flatMap { sym ->
                // get the (non-empty) list of declarations within the unit
                val decls = state.ast
                    .declarationsForSymbol(sym)
                    .filterIsInstance<UnitDeclaration.Valuelike<*, *>>()

                // if the unit contains a definition for the symbol,
                // then it is not an external dependency.
                if (decls.any { it.isDefinition }) setOf()
                else setOf(DeclarationInUnit(state.tuid, decls.first()))
            }
    }

    // This throws its exceptions, which is why we keep this private.
    // Preferring to return exceptions via Either for consistency
    private fun closureOfOrThrow(graph: LinkGraph): LinkGraph {
        // Initialize the worklist from the graph.
        // The worklist contains *declarations* without definitions in the same unit that need to be resolved.
        val unbound: MutableSet<DeclarationInUnit> = graph.dependencies.entries
            .flatMap { (tuid, linking) -> linking.unbound.map { DeclarationInUnit(tuid, it) }}
            .toMutableSet()

        // Initialize the edgeset from the given graph.
        // We will iteratively grow the edgeset by resolving items from the worklist,
        // until the set of unbound symbols is empty.
        val links: MutableList<Link> = graph
            .dependencies
            .entries
            .flatMapTo(mutableListOf()) { it.value.bound }

        // We keep track of symbols that we cannot unambiguously resolve to definitions.
        val failed = mutableMapOf<DeclarationInUnit, Unresolved>()

        // Now we do the work.
        //
        // While we loop, we pop items from the worklist and try to resolve them.
        // If this succeeds then we extend the graph.
        // This also grows the worklist, because the definitions that we resolve to can have their own dependencies.
        // If it fails, we add it to the map of failed resolutions.
        //
        // We avoid putting things on the worklist that we already tried to resolve,
        // The set of symbols in the index is finite, so this means that
        // eventually we run out of symbols on the worklist and the algorithm terminates.
        val visited = mutableSetOf<DeclarationInUnit>()
        while (unbound.isNotEmpty()) {
            // remove an item from the work list
            val decl = unbound
                .first()
                .apply { unbound.remove(this) }

            if (decl in visited) continue
            else visited.add(decl)

            // try to resolve the prototype using the index
            val options: Set<Index.Entry> = index.resolve(decl.proto)

            // check if we have an unambiguous resolution
            if (options.size == 1) {
                // link the previously unbound symbol to the result of resolution in the index.
                val def = options.first().let { DeclarationInUnit(it.tuid, it.proto) }

                // extend the links with the newly found link
                links.add(Link(decl, def))

                // add the dependencies of the linked definition to the worklist
                // after filtering only those declarations that we have not worked on before
                val work = undefinedDependenciesOfOrThrow(def)
                    .filter { it !in visited }

                unbound.addAll(work)
            } else {
                // if the resolution is not ambiguous,
                // then we extend the set of failures for the unit.
                val failure = Unresolved(decl, options)
                failed[decl] = failure
            }

        }

        return LinkGraph(links, failed.keys)
    }
}