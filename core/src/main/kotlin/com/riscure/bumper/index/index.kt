package com.riscure.bumper.index

import arrow.core.*
import com.riscure.bumper.ast.*

open class IndexException(msg: String, cause: Exception? = null) : Exception(msg, cause)
class MissingDefinition(name: String): IndexException("No definition found for name '$name'")
class MultipleDefinitions(name: String, matches: Collection<SymbolInfo>): IndexException(
    """
    Multiple definitions found for name '$name':
    ${matches.joinToString(separator="\n\t") { "- ${it.unit.main}:${it.tlid.name}" }}
    """.trimIndent())

data class SymbolInfo(
    val symbol : Symbol,
    val hasDefinition: Boolean,
    val storage: Storage,
) {
    val name get() = tlid.name
    val kind get() = tlid.kind
    val unit get() = symbol.unit
    val tlid get() = symbol.tlid
}

data class Index(val symbolsByName: Map<Ident, Set<SymbolInfo>> = mapOf()) {

    /**
     * Number of symbols in the index.
     */
    val size: Int get() = symbolsByName.foldLeft(0) { acc, (_, syms) -> acc + syms.size }

    val symbols: Set<SymbolInfo> by lazy {
        symbolsByName.entries
            .flatMap { it.value }
            .toSet()
    }

    operator fun get(name: Ident): Set<SymbolInfo> =
        symbolsByName[name]
            .toOption()
            .getOrElse { setOf() }

    operator fun get(tlid: TLID): Set<SymbolInfo> =
        symbolsByName[tlid.name]
            .toOption()
            .map { it.filter { s -> s.tlid == tlid }.toSet() }
            .getOrElse { setOf() }

    fun getDefinitions(name: String) = this[name].filter { it.hasDefinition }

    /**
     * Filter the symbols in the index
     */
    fun filter(f: Predicate<SymbolInfo>) = copy(
        symbolsByName = symbolsByName.mapValues { entry -> entry.value.filter { f(it) }.toSet() }
    )

    /**
     * Map the symbols in the index
     */
    fun map(transform: (SymbolInfo) -> SymbolInfo) = copy(
        symbolsByName = symbolsByName
            .mapValues { entry -> entry.value
                .map { transform(it) }
                .toSet()
            }
    )

    /**
     * Flatmap the symbols in the index
     */
    fun flatMap(transform: (SymbolInfo) -> Set<SymbolInfo>) = copy(
        symbolsByName = symbolsByName
            .mapValues { entry -> entry.value
                .flatMap { transform(it) }
                .toSet()
            }
    )

    /**
     * Try to resolve a entity identifier.
     * @throws IndexException whenever the resolution fails or when it is ambiguous.
     */
    fun getUnambiguousDefinition(id: TLID): Either<IndexException, SymbolInfo> {
        val defs = getDefinitions(id.name)
        return when (defs.size) {
            0 -> MissingDefinition(id.name).left()
            1 -> defs.first().right()
            else -> MultipleDefinitions(id.name, defs).left()
        }
    }

    /**
     * Create a new index out of {@code this} and {@code that}
     */
    fun combine(that: Index) = Index(
        symbolsByName.zip(that.symbolsByName) { key, left, right -> left + right }
    )

    companion object {
        @JvmStatic
        fun merge(indices: Collection<Index>): Index = create(
            indices
                .flatMap { it.symbols }
                .toSet()
        )

        @JvmStatic
        fun create(symbols: Collection<SymbolInfo>): Index {
            val symtab = mutableMapOf<String, MutableSet<SymbolInfo>>()
            for (symbol in symbols) {
                val syms = symtab.getOrElse(symbol.tlid.name) { mutableSetOf() }
                syms.add(symbol)
                symtab[symbol.tlid.name] = syms
            }

            return Index(symtab)
        }
    }
}