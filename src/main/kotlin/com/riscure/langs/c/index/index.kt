package com.riscure.langs.c.index

import arrow.core.*
import arrow.typeclasses.Monoid
import arrow.typeclasses.Semigroup
import com.riscure.langs.c.ast.TLID
import com.riscure.langs.c.ast.TranslationUnit
import java.nio.file.Path

/* Uniquely identify a translation unit */
data class TUID(
    /* Translation units are identified by their main file */
    val main: Path
)

/* Uniquely identify a symbol across translation units */
data class Symbol(
    val unit: TUID,
    val entity: TLID
)

open class IndexException(msg: String, cause: Exception? = null) : Exception(msg, cause)
class MissingDefinition(name: String): IndexException("No definition found for name '$name'")
class MultipleDefinitions(name: String): IndexException("Multiple definitions found for name '$name'")

data class Index(val symbolsByName: Map<String, Set<Symbol>> = mapOf()) {

    val symbols: Set<Symbol> by lazy {
        symbolsByName.entries
            .flatMap { it.value }
            .toSet()
    }

    fun getDefinitions(id: TLID) =
        symbolsByName[id.name]
            .toOption()
            .map { match -> match.filter { it.entity.kind.hasDefinition } }
            .getOrElse { setOf() }

    fun filter(f: Predicate<Symbol>) = copy(
        symbolsByName = symbolsByName.mapValues { entry -> entry.value.filter { f(it) }.toSet() }
    )

    fun getUnambiguousDefinition(id: TLID): Either<IndexException, Symbol> {
        val defs = getDefinitions(id)
        return when (defs.size) {
            0 -> MissingDefinition(id.name).left()
            1 -> defs.first().right()
            else -> MultipleDefinitions(id.name).left()
        }
    }

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