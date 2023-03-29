package com.riscure.bumper.index

import arrow.core.getOrElse
import arrow.core.zip
import com.riscure.bumper.analyses.LinkAnalysis
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Index.Entry
import java.nio.file.Path

/**
 * An [Index] maps identifiers to a set of matching [Entry]s.
 * It only records value definitions (functions, globals) that are visible across unit boundaries (non-static).
 */
data class Index(val symbols: Map<Ident, Set<Entry>>) {
    data class Entry(
        val tuid: TUID,
        val proto: Prototype,
        val meta: Meta
    ) {
        val name: String get() = proto.tlid.name
        val symbol: Symbol get() = Symbol(tuid, proto.tlid)

        /**
         * Get the most original source location known.
         * Herein we trust the parsed presumed location outputted usually by the preprocessor.
         */
        val originalLocation get() =
            meta.presumedLocation.map { it.sourceFile } .getOrElse { tuid.main }
    }

    fun plus(other: Index): Index =
        Index(symbols.zip(other.symbols) { _, l, r -> l + r })

    /**
     * Find [match]ing definitions in the index for the given [prototype].
     */
    fun resolve(prototype: Prototype): Set<Entry> =
        symbols
            .getOrDefault(prototype.tlid.name, setOf())
            .filter { decl -> match(prototype.type, decl.proto.type) }
            .toSet()

    companion object {
        /**
         * We don't have a function yet to check if two types are 'compatible'
         * in the vague C sense of that word. So for now we do a weak check 'match'.
         */
        private fun match(left: Type, right: Type) = when (left) {
            is Type.Fun ->
                right is Type.Fun &&
                        left.params.size == right.params.size &&
                        left.vararg == right.vararg
            else -> true
        }

        fun of() = Index(mapOf())

        /**
         * Index a single unit
         */
        fun of(unit: TranslationUnit<*, *>): Index {
            val exports = LinkAnalysis.objectInterface(unit).exports
            val symbols = exports.associateBy({ it.ident }) { export ->
                setOf(Entry(unit.tuid, export.prototype(), export.meta))
            }

            return Index(symbols)
        }

        /**
         * Combine a collection of [indexes] into one.
         */
        fun merge(indexes: Collection<Index>): Index {
            val index: MutableMap<Ident, Set<Entry>> = mutableMapOf()

            for (other in indexes) {
                for ((p, tuids) in other.symbols) {
                    index.merge(p, tuids) { left, right -> left + right }
                }
            }

            return Index(index)
        }
    }
}