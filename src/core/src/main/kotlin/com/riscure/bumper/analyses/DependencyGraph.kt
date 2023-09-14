package com.riscure.bumper.analyses

import arrow.typeclasses.Monoid
import com.riscure.bumper.index.Symbol

interface MutGraphMixin {
    fun add(from: Symbol, to: Sequence<Symbol>): Unit
}

/**
 * Update this mutable graph to the union of this graph with [other].
 */
fun <G:MutGraphMixin> G.absorp(other: DependencyGraph): G = apply {
    for ((k, otherDeps) in other.dependencies) {
        // merge the edge-sets of every node
        add(k, otherDeps.asSequence())
    }
}

/**
 * Mutable implementation of a [DependencyGraph].
 *
 * @see DependencyGraph
 */
data class MutDependencyGraph(
    /**
     * A mapping of nodes in this dependency graph to their dependencies.
     * Every node should be represented in the key-set of this mapping.
     *
     * It is the responsibility of the one constructing the graph to initialize
     * this map such that the invariant is satisfied. You can use [DependencyGraph.build]
     * to complete an incomplete mapping.
     */
    override val dependencies: MutableMap<Symbol, MutableSet<Symbol>> = mutableMapOf(),
) : DependencyGraph(), MutGraphMixin {

    /**
     * Add dependencies from [from] to all symbols in [to].
     */
    override fun add(from: Symbol, to: Sequence<Symbol>) {
        dependencies.compute(from) { _, deps ->
            when {
                deps == null -> to.toMutableSet()
                else         -> deps.apply { addAll(to) }
            }
        }
    }
}

/**
 * A [DependencyGraph] describes the dependencies between symbols, whether they be
 * functions, variables, types, etc.
 *
 * The graph is represented as an edge-set: the [dependencies] map gives a set of
 * symbols that a given symbol depends on.
 *
 * A [DependencyGraph] can be used for graphs for a single or multiple translation units.
 */
abstract class DependencyGraph {
    /**
     * A mapping for _every node_ to the nodes that is has dependencies on.
     */
    abstract val dependencies: Map<Symbol, Set<Symbol>>

    val nodes get() = dependencies.keys

    fun union(other: DependencyGraph) = with (graphMonoid) { combine(other) }

    /**
     * Find the shortest path from [from] to [to].
     */
    fun findPath(from: Symbol, to: Symbol): List<Symbol> {
        // All responsibility for bugs in the following implementation of Dijkstra's shortest
        // path algorithm is for ChatGPT.

        // a map with the shortest known distance from [from] to the key.
        // This map is complete for the whole set of nodes
        val distances = mutableMapOf<Symbol, Int>()

        // a mapping from nodes N to the neighbor that precedes it on the shortest path towards N
        val via = mutableMapOf<Symbol, Symbol>()

        // the worklist
        val unvisited = mutableSetOf<Symbol>()

        // Initialize distances and unvisited nodes
        for ((node, _) in dependencies) {
            distances[node] = Int.MAX_VALUE
            unvisited.add(node)
        }
        distances[from] = 0

        while (unvisited.isNotEmpty()) {
            val current = unvisited.minByOrNull { distances[it]!! }!!
            unvisited.remove(current)

            // when we found the node we were looking for, then we have the
            // shortest path to that node.
            if (current == to) {
                // Reconstruct the shortest path
                val path = mutableListOf<Symbol>()
                var node: Symbol? = to
                while (node != null) {
                    path.add(0, node)
                    node = via[node]
                }
                return path
            }

            for (neighbor in dependencies[current]!!) {
                 // compute the new distances
                val newDistance = distances[current]!! + 1
                // check if we found a shorter path towards [current]
                if (newDistance < distances.getOrDefault(neighbor, Int.MAX_VALUE)) {
                    distances[neighbor] = newDistance
                    via[neighbor] = current
                }
            }
        }

        return emptyList() // No path found
    }

    /**
     * Performs a reachability analysis on the dependency graph, returning a [Subgraph] view
     * on this graph that depicts the reachable subgraph.
     *
     * @param roots are the nodes from which we start the reachability analysis.
     * @return the subgraph of nodes that are reachable in this graph from the given set of [roots].
     */
    open fun reachable(roots: Set<Symbol>) =
        ReflexiveTransitiveSubgraph.of(this, roots.asSequence())

    companion object {

        /**
         * Smart constructor that ensures that the keyset of nodes is complete,
         * even if you pass a mapping where some nodes only appear on the right-hand side.
         */
        fun build(edges: Map<Symbol, Set<Symbol>>): DependencyGraph = MutDependencyGraph().apply {
            val nodes = mutableSetOf<Symbol>()
            for ((from, toSet) in edges) {
                dependencies[from] = toSet.toMutableSet()
                nodes.addAll(toSet)
            }

            for (node in nodes) {
                dependencies.putIfAbsent(node, mutableSetOf())
            }
        }

        object graphMonoid: Monoid<DependencyGraph> {

            override fun DependencyGraph.combine(b: DependencyGraph): DependencyGraph =
                MutDependencyGraph()
                    .absorp(this)
                    .absorp(b)

            override fun empty(): DependencyGraph = MutDependencyGraph()
        }

        /** Construct an empty dependency graph */
        @JvmStatic fun empty(): DependencyGraph = graphMonoid.empty()

        /** Combine a list of dependency graphs into one */
        @JvmStatic fun union(graphs: List<DependencyGraph>) = graphMonoid.fold(graphs)

    }
}

/**
 * An immutable subgraph view on a [DependencyGraph].
 */
open class Subgraph(
    internal val superGraph: DependencyGraph,
    open val selection: Set<Symbol> = emptySet()
): DependencyGraph() {

    operator fun contains(sym: Symbol): Boolean = sym in selection

    /**
     * Grow the selection to the reflexive transitive closure over the edges in the [superGraph],
     * returning a new subgraph view.
     */
    fun reflexiveTransitiveClosure() = ReflexiveTransitiveSubgraph
        .of(superGraph, selection.asSequence())

    override val dependencies: Map<Symbol, Set<Symbol>>
        get() {
            val sub = mutableMapOf<Symbol, Set<Symbol>>()
            for ((node, edges) in superGraph.dependencies) {
                if (node !in selection) continue
                sub[node] = edges.filterTo(mutableSetOf()) { it in selection }
            }

            return sub
        }

}

/**
 * A mutable reflexive-transitive closure view on an immutable [DependencyGraph].
 *
 * That is: this view assumes that the underlying dependency graph does not change.
 * If you do change the underlying graph, you can either update this view incrementally
 * by calling [maintain] if only edges were added. Otherwise, you have to regenerate the view.
 */
class ReflexiveTransitiveSubgraph private constructor(
    superGraph: DependencyGraph,
    private val mutSelection: MutableSet<Symbol> = mutableSetOf()
): Subgraph(superGraph, mutSelection) {

    /**
     * Add [nodes] to the reflexive-transitive [selection] alongside its closure.
     */
    fun select(nodes: Sequence<Symbol>) = apply {
        val worklist = nodes.toMutableList()

        while (worklist.isNotEmpty()) {
            val next = worklist.removeAt(0)

            // if we already have this node selected, then we're good
            if (next in selection) continue

            // update the selection
            mutSelection.add(next)

            // add the neighbours to the worklist
            worklist.addAll(superGraph.dependencies.getOrDefault(next, emptySet()))
        }

    }

    /**
     * Update this view on the occasion of an update of the underlying [superGraph].
     */
    fun maintain(newEdges: Sequence<Pair<Symbol, Symbol>>) = apply {
        // we only have to take into account new edges poking out of the selection
        val pokingOut = newEdges
            .filter { (from, to) -> from in selection && to !in selection }

        // extend the selection to the newly reachable nodes and their refl.-trans. closure.
        select(pokingOut.map { (_, to) -> to})
    }

    companion object {
        fun of(superGraph: DependencyGraph, selection: Sequence<Symbol>) =
            ReflexiveTransitiveSubgraph(superGraph).select(selection)
    }

}