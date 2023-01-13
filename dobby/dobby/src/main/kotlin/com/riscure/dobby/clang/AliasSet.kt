package com.riscure.dobby.clang

import arrow.core.getOrElse
import arrow.core.toOption

/**
 * Simple union-find node for option aliasing.
 */
class AliasSet(val self: OptionSpec) {
    /** start of path towards the representative alias */
    private var canon: AliasSet

    init {
        // tie the knot.
        // This reflects that we want to construct a *reflexive* transitive closure.
        this.canon = this
    }

    fun alias(canonical: AliasSet) = apply { canon = canonical }

    fun representative(): AliasSet {
        val repr = if (this == canon) this else canon.representative()

        // Flexing some path compression on the backward edge.
        this.canon = repr

        return repr
    }
}

/** Reflexive, Symmetric, Transitive closure of alias relationship */
data class Aliasing(
    private val closure: Map<String, Set<OptionSpec>>
) {
    fun aliases(key: String)     = closure[key].toOption()
    fun aliases(opt: OptionSpec) = closure[opt.key].toOption()
    fun areAliasing(o1: OptionSpec, o2: OptionSpec) =
        aliases(o1)
            .map { it.contains(o2) }
            .getOrElse { false }

    companion object {
        /**
         * Construct a fully path compressed alias graph
         */
        fun build(spec: Spec, optionsByKey: Map<String, OptionSpec>): Aliasing {
            // compute the nodes
            val alis: Map<String, AliasSet> = optionsByKey
                .mapValues { (_, o) -> AliasSet(o) }

            // add the edges
            alis.forEach { (key, set) ->
                set.self.aliasFor.tap { alias ->
                    val canonical = alis.getOrElse(alias.forKey) { throw spec.InvalidKey(alias.forKey) }
                    set.alias(canonical)
                }
            }

            // index by representative
            val byRepr = alis.values
                .groupBy{ it.representative().self.key }
                .mapValues { (_, it) -> it.map { it.self }.toSet()}

            // compute the closure
            return Aliasing(alis.mapValues { (k, node) ->
                byRepr[node.representative().self.key]!!
            })
        }
    }
}