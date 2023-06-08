package com.riscure.bumper.data

import arrow.core.Option
import arrow.core.toOption

internal typealias MutEdgeset<N>  = MutableSet<N>
internal typealias MutGraph<N>    = MutableMap<N, MutEdgeset<N>>

interface AcyclicGraph<N> {
    operator fun get(key: N): Option<Set<N>>

    /**
     * Construct the reverse graph, flipping all the directed edges.
     */
    fun reverse(): AcyclicGraph<N>

    /**
     * Return the nodes of this graph in topological ordering,
     * starting with the nodes without dependencies.
     */
    fun topologicalSort(): List<N>
}

/**
 * A directed acyclic graph.
 */
class MutableAcyclicGraph<N>: AcyclicGraph<N> {
    /**
     * The edge-set of the graph
     */
    val edges: MutGraph<N> = mutableMapOf()

    override operator fun get(key: N) = edges[key].toOption()

    fun addNode(node: N) = apply {
        edges[node] = mutableSetOf()
    }

    /**
     * Add an edge from [from] to [to].
     *
     * The implementation does not check if this break the promise that this graph is acyclic,
     * so the caller should cooperate to maintain that property.
     */
    fun addEdge(from: N, to: N) = apply {
        // add the new edge
        when (val edgeset = edges[from]) {
            null -> edges[from] = mutableSetOf(to)
            else -> edgeset.add(to)
        }
    }

    override fun reverse(): MutableAcyclicGraph<N> = MutableAcyclicGraph<N>().apply {
        val backward = this@MutableAcyclicGraph.edges

        for ((from, dependencies) in backward) {
            for (to in dependencies) {
                when (val forward = edges[to]) {
                    null -> edges[to] = mutableSetOf(from)
                    else -> forward.add(from)
                }
            }
        }
    }

    override fun topologicalSort(): List<N> {
        val result = mutableListOf<N>()

        // get the frontier of our search for leafs.
        // In an acyclic graph there should always be a non-empty frontier.
        val frontier : MutableSet<N> = mutableSetOf() // worklist of nodes with 0 dependencies
        val remainder: MutGraph<N> = mutableMapOf()   // nodes with >= 1 dependency
        for ((key, es) in edges.entries) {
            when {
                es.isEmpty() -> frontier.add(key)
                else         -> remainder[key] = es
            }
        }

        // the reverse graph makes it easy to update the frontier
        val rev = reverse().edges

        // the frontier should remain non-empty until we have processed the entire graph.
        // This follows from the fact that all non-connected components are invariantly in the
        // frontier and the fact that the graph is known to be acyclic.
        while (frontier.isNotEmpty()) {
            // get a node without dependencies from the worklist
            val key = frontier.first()

            // move it from the frontier to the result
            frontier.remove(key)
            result.add(key)

            // remove it from the dependencies of all its dependants
            for (dependant in rev[key]!!) {
                when (val depDeps = remainder[dependant]) {
                    null -> {}
                    else -> {
                        depDeps.remove(key)

                        // update the frontier if this dependant now has zero deps
                        if (depDeps.isEmpty()) frontier.add(key)
                    }
                }
            }
        }

        return result
    }
}