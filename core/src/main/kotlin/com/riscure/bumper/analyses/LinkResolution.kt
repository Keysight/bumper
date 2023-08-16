package com.riscure.bumper.analyses

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.right
import com.riscure.bumper.ast.TLID
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.index.TUID

abstract class LinkError: Exception() {
    data class DuplicateDefinition(
        val tlid: TLID,
        val first: DeclarationInUnit,
        val second: DeclarationInUnit
    ): LinkError() {
        override val message: String
            get() =
                """
                    Duplicate definition of ${tlid.pretty}:
                    - ${first.tuid.main}
                    - ${second.tuid.main}
                """.trimIndent()
    }
}

/**
 * [LinkResolution] implements an incremental link-time dependency analysis,
 * figuring out which declaration is linked to which definition. The resolution algorithm
 * is entirely based on value names and does not take into account type mismatches.
 *
 * It maintains the state of the analysis, and exposes an API that models adding units
 * to a link target, accumulating [Link]s.
 *
 * The analysis support minimization, only adding value dependencies that are reachable
 * from a set of root values.
 */
class LinkResolution(
    /**
     * For every known unit a set of links
     */
    private val defined: MutableMap<TUID, MutableSet<Link>> = mutableMapOf(),

    /**
     * A set of declarations without known definitions
     */
    private val undefined: MutableSet<DeclarationInUnit> = mutableSetOf(),

    /**
     * An index with every known visible symbol in the link target.
     */
    private val exportIndex: MutableMap<TLID, DeclarationInUnit> = mutableMapOf()
) {

    // read-only access to the state
    val unbound: Sequence<DeclarationInUnit> get() = undefined.asSequence()
    val bound: Map<TUID, Set<Link>> get() = defined

    /**
     * Get the [LinkGraph] representing the currently modeled link target.
     */
    fun linkgraph() = LinkGraph(bound.asSequence().flatMap { it.value.asSequence() }, unbound)

    fun <E,S> link(unit: TranslationUnit<E, S>) = link(objectInterface(unit))

    /**
     * Add [unit] to the link target, updating the [exportIndex], the [undefined]
     * symbols, and resolving any import of [unit] against other units in the modeled
     * current target.
     *
     * @return a [LinkError] in case of duplicate definitions in the target,
     *   or a set of newly added [Link]s as a result of linking [unit].
     */
    fun <E,S> link(unit: UnitInterface<E, S>): Either<LinkError, Set<Link>> = try {
        val (_, imports, exports) = unit

        // add the exports to the index
        for (exp in exports) {
            val clashing = exportIndex[exp.tlid]
            val export = DeclarationInUnit(unit.tuid, exp)
            if (clashing != null) {
                throw LinkError.DuplicateDefinition(exp.tlid, clashing, export)
            }
            exportIndex[exp.tlid] = export
        }

        // add the imports to the worklist
        // and retry resolving anything on the worklist against the new exports
        undefined.addAll(
            imports
                .asSequence()
                .map { DeclarationInUnit(unit.tuid, it) }
        )

        resolve().right()
    } catch(err: LinkError) { err.left() }

    /**
     * Try to resolve any [undefined] symbol in the current [exportIndex].
     * This updates [undefined] and [defined].
     *
     * @return newly created links
     */
    private fun resolve(): Set<Link> = run {
        // we cannot directly update unbound while iterating it,
        // so we keep track of what we bind
        val newBound = mutableSetOf<DeclarationInUnit>()
        val newLinks = mutableSetOf<Link>()
        for (import in undefined) {
            // symbol resolution is entirely based on value names at the moment.
            val match = exportIndex[import.tlid]
            if (match != null) {
                val link = Link(import, match)
                newBound.add(import)
                newLinks.add(link)
                defined.merge(import.tuid, mutableSetOf(link)) { l, r ->
                    l.apply { addAll(r) }
                }
            }
        }

        // update the worklist
        undefined.removeAll(newBound)

        newLinks
    }


    companion object {
        /**
         * Wholesale link resolution on [units], returning a [LinkGraph] or a [LinkError].
         */
        @JvmStatic
        fun <E, S> linkgraph(units: Sequence<TranslationUnit<E, S>>) = try {
            LinkResolution()
                .apply {
                    for (unit in units) {
                        link(unit).getOrHandle { throw it }
                    }
                }
                .linkgraph()
                .right()
        } catch (err: LinkError) { err.left() }

        // for backward-compatibility
        @JvmStatic
        fun <E, S, C:Collection<TranslationUnit<E,S>>> linkgraph(units: C) = linkgraph(units.asSequence())
    }
}
