package com.riscure.bumper.analyses

import arrow.typeclasses.Monoid
import com.riscure.bumper.index.Symbol

/**
 * A [DependencyGraph] describes the dependencies between symbols, whether they be
 * functions, variables, types, etc.
 *
 * The graph is represented as an edge-set: the [dependencies] map gives a set of
 * symbols that a given symbol depends on.
 *
 * A [DependencyGraph] can be used for graphs for a single or multiple translation units.
 */
data class DependencyGraph
// we use a private constructor and have callers use the DependencyGraph.invoke instead,
// which helps them construct it correctly.
private constructor(
    /**
     * A mapping for _every node_ to the nodes that is has dependencies on.
     */
    val dependencies: Map<Symbol, Set<Symbol>>
) {

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
     * Performs a reachability analysis on the dependency graph.
     *
     * @param roots are the nodes from which we start the reachability analysis.
     * @return the subgraph of nodes that are reachable in this graph
     *         from the given set of [roots].
     */
    fun reachable(roots: Set<Symbol>): DependencyGraph {
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

        return copy(dependencies = dependencies.filter { (k, _) -> reachable.contains(k) })
    }

    companion object {

        /**
         * Smart constructor that ensures that the keyset of nodes is complete,
         * even if you pass a mapping where some nodes only appear on the right-hand side.
         */
        operator fun invoke(edges: Map<Symbol, Set<Symbol>>): DependencyGraph {
            val completion = mutableMapOf<Symbol, Set<Symbol>>()
            val nodes = mutableSetOf<Symbol>()
            for ((from, toSet) in edges) {
                completion[from] = toSet
                nodes.addAll(toSet)
            }

            for (node in nodes) {
                completion.putIfAbsent(node, emptySet())
            }

            return DependencyGraph(completion)
        }

        object graphMonoid: Monoid<DependencyGraph> {

            override fun DependencyGraph.combine(b: DependencyGraph): DependencyGraph {
                val result = mutableMapOf<Symbol, Set<Symbol>>()

                for (k in this.dependencies.keys + b.dependencies.keys) {
                    result[k] = this.dependencies.getOrDefault(k, setOf()) + b.dependencies.getOrDefault(k, setOf())
                }

                return DependencyGraph(result)
            }

            override fun empty() = DependencyGraph(mapOf())
        }

        /** Construct an empty dependency graph */
        @JvmStatic fun empty() = graphMonoid.empty()

        /** Combine a list of dependency graphs into one */
        @JvmStatic fun union(graphs: List<DependencyGraph>) = graphMonoid.fold(graphs)

    }
}