@file:JvmName("Validation")

package com.riscure.dobby.clang

import arrow.core.Either
import java.nio.file.Path

/**
 * A monadic type for refining a value until it passes some test,
 * while accumulating messages describing what was invalid.
 */
data class Validated<T>(
    val value: T,
    val messages: List<String> = listOf()
) {
    fun flatMap(f: (T) -> Validated<T>) =
        f(value).let { it.copy(messages = this.messages.plus(it.messages)) }
}

/**
 * Removes duplicate entries (entries whose main source file clash with earlier entry).
 */
fun CompilationDb.dropDuplicates(): Validated<CompilationDb> =
    Validated(this)
        .flatMap { db ->
            val entrymap = mutableMapOf<Path, CompilationDb.Entry>()
            val warnings = mutableListOf<String>()

            for (entry in db.entries) {
                if (entry.resolvedMainSource !in entrymap) {
                    entrymap[entry.resolvedMainSource] = entry
                } else {
                    warnings.add("Dropped duplicate entry for file '${entry.resolvedMainSource}'.")
                }
            }

            Validated(CompilationDb(entrymap.values.toList()), warnings)
        }