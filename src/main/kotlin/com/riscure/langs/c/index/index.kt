package com.riscure.langs.c.index

import arrow.core.*
import com.riscure.langs.c.ast.TLID
import java.nio.file.Path

/* Uniquely identify a translation unit */
data class TUID(
    /* Translation units are identified by their main (preprocessed) file */
    val main: Path
)

/* Uniquely identify a symbol across translation units */
data class Symbol(
    val unit: TUID,
    val entity: TLID
) {
    val name get() = entity.name
    val kind get() = entity.kind

    companion object {
        @JvmStatic
        fun prototype(unit: TUID, name: String): Symbol = Symbol(unit, TLID.prototype(name))

        @JvmStatic
        fun function(unit: TUID, name: String): Symbol = Symbol(unit, TLID.function(name))
    }
}

open class IndexException(msg: String, cause: Exception? = null) : Exception(msg, cause)
class MissingDefinition(name: String): IndexException("No definition found for name '$name'")
class MultipleDefinitions(name: String, matches: Collection<Symbol>): IndexException(
    """
    Multiple definitions found for name '$name':
    ${matches.joinToString(separator="\n\t") { "- ${it.unit.main}:${it.entity.name}" }}
    """.trimIndent())

data class Index(val symbolsByName: Map<String, Set<Symbol>> = mapOf()) {

    /**
     * Number of symbols in the index.
     */
    val size: Int get() = symbolsByName.foldLeft(0) { acc, (_, syms) -> acc + syms.size }

    val symbols: Set<Symbol> by lazy {
        symbolsByName.entries
            .flatMap { it.value }
            .toSet()
    }

    operator fun get(name: String): Set<Symbol> =
        symbolsByName[name]
            .toOption()
            .getOrElse { setOf() }

    operator fun get(tlid: TLID): Set<Symbol> =
        symbolsByName[tlid.name]
            .toOption()
            .map { it.filter { s -> s.entity.kind == tlid.kind }.toSet() }
            .getOrElse { setOf() }

    fun getDefinitions(name: String) = this[name].filter { it.entity.kind.hasDefinition }

    /**
     * Filter the symbols in the index
     */
    fun filter(f: Predicate<Symbol>) = copy(
        symbolsByName = symbolsByName.mapValues { entry -> entry.value.filter { f(it) }.toSet() }
    )

    /**
     * Map the symbols in the index
     */
    fun map(transform: (Symbol) -> Symbol) = copy(
        symbolsByName = symbolsByName
            .mapValues { entry -> entry.value
                .map { transform(it) }
                .toSet()
            }
    )

    /**
     * Flatmap the symbols in the index
     */
    fun flatMap(transform: (Symbol) -> Set<Symbol>) = copy(
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
    fun getUnambiguousDefinition(id: TLID): Either<IndexException, Symbol> {
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
        fun merge(indices: Collection<Index>): Index = Index.create(
            indices
                .flatMap { it.symbols }
                .toSet()
        )

        @JvmStatic
        fun create(symbols: Collection<Symbol>): Index {
            val symtab = mutableMapOf<String, MutableSet<Symbol>>()
            for (symbol in symbols) {
                val syms = symtab.getOrElse(symbol.entity.name) { mutableSetOf() }
                syms.add(symbol)
                symtab[symbol.entity.name] = syms
            }

            return Index(symtab)
        }
    }
}