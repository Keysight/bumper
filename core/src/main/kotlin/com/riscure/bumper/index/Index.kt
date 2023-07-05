@file:UseSerializers(NothingSerializer::class)
package com.riscure.bumper.index

import com.riscure.bumper.serialization.NothingSerializer

import arrow.core.*
import com.riscure.bumper.analyses.objectInterface
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.Index.Entry
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.*
import java.io.*

/**
 * An [Index] maps identifiers to a set of matching [Entry]s.
 *
 * It only records entries for value (functions, globals) **definitions**
 * that are visible across unit boundaries (non-static).
 *
 * For each definition, only the prototype is recorded in the index,
 * so despite each entry _representing_ a definition, entry.isDefinition will be false for each.
 */
@Serializable
data class Index(val symbols: Map<Ident, Set<Entry>>) {
    @Serializable
    data class Entry(
        val tuid: TUID,

        /**
         * The prototype of a *definition* in [tuid].
         *
         * Because we only record the prototype, `proto.isDefinition` will be false,
         * but do not be misleaded by this. The prototype represents a *definition*
         * and the [meta]data belongs to that definition.
         */
        val proto: UnitDeclaration.Valuelike<Nothing,Nothing>,

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

    fun plus(other: Index): Index = merge(listOf(this, other))

    /**
     * Find definitions in the index for the given [id].
     *
     */
    fun resolve(id: Ident): Set<Entry> = symbols.getOrDefault(id, setOf())

    /**
     * Find [match]ing definitions in the index for the given [prototype].
     * The function [match] only performs a weak type check, checking only arity.
     */
    fun findCandidateDefinitions(prototype: UnitDeclaration.Valuelike<*,*>): Set<Entry> =
        symbols
            .getOrDefault(prototype.tlid.name, setOf())
            .filter { decl -> match(prototype.type, decl.proto.type) }
            .toSet()

    /**
     * Serialize the index as a CBOR binary to [stream].
     */
    @OptIn(ExperimentalSerializationApi::class)
    fun write(stream: OutputStream) {
        Cbor
            .encodeToByteArray(serializer(), this)
            .let {
                stream.write(it)
                stream.flush()
            }
    }

    companion object {
        /**
         * Read and deserialize and index from a CBOR binary [stream].
         */
        @OptIn(ExperimentalSerializationApi::class)
        fun read(stream: InputStream): Either<Exception, Index> = try {
            Cbor
                .decodeFromByteArray<Index>(stream.readBytes())
                .right()
        }
        catch (err : IllegalArgumentException) { err.left() }
        catch (err : SerializationException)   { err.left() }

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
            val exports = objectInterface(unit).exports
            val symbols = exports.associateBy({ it.ident }) { export ->
                setOf(Entry(unit.tuid, export.prototype, export.meta))
            }

            return Index(symbols)
        }

        /**
         * Index a bunch of [units]
         */
        fun of(units: Sequence<TranslationUnit<*, *>>): Index =
            units
                .flatMap { unit ->
                    val exports = objectInterface(unit).exports
                    exports
                        .asSequence()
                        .map { export -> Entry(unit.tuid, export.prototype, export.meta) }
                }
                .groupBy { it.name }
                .mapValues { (_, value) -> value.toSet() }
                .let { Index(it) }

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