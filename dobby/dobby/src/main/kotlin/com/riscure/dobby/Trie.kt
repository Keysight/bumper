package com.riscure.dobby

import arrow.core.*
import com.riscure.dobby.clang.OptionSpec
import stdlibpp.foldM

/* Every spec that matches the input is paired with the remaining input */
private typealias Matches = Set<Pair<OptionSpec,String>>

/**
 * A trie represents a collection of prefixes with possible overlap
 * in such a way that we can efficiently find the longest matching prefix
 * given an input string.
 */
class Trie(
    private var match: MutableSet<OptionSpec> = mutableSetOf(),
    private val children: MutableMap<Char, Trie> = mutableMapOf()
) {
    companion object {
        fun create(opts: Iterable<OptionSpec>): Either<String, Trie> =
            Trie().let { result ->
                for (opt in opts) {
                    when (val r = result.insert(opt)) {
                        is Either.Left  -> return r
                        is Either.Right -> continue
                    }
                }

                return result.right()
            }
    }

    fun longestPrefix(input: String, longestSoFar: Matches = setOf()): Matches {
        // found new longest match?
        val longest: Matches = match
            .map { Pair(it, input) }
            .toSet()
            .ifEmpty { longestSoFar }

        // when we're out of input then we are done
        if (input.isEmpty()) {
            return longest
        }

        // if we still have input, we try to find a longer match
        // by searching for a transition to parse another char of input
        val edge = children.entries
            .find { (k, v) -> input.startsWith(k) }
            .toOption()

        return when (edge) {
            // If we found a transition, traverse it
            // There cannot be multiple, because edges have disjoint labels.
            is Some -> edge.value.value.longestPrefix(input.drop(1), longest)
            // if we didn't, then we return the longest match so far
            else    -> longest
        }
    }

    fun insert(opt: OptionSpec): Either<String, Trie> =
        opt
            .allAppearances()
            .foldM(this) { acc, appearance ->
                acc.insert(appearance, opt)
            }

    private fun insertLeaf(opt: OptionSpec): Trie =
        this.match.add(opt).let { this }

    private fun insert(nameSuffix: String, opt: OptionSpec): Either<String, Trie> {
        // base step
        if (nameSuffix.isEmpty()) {
            return insertLeaf(opt).right()
        }

        // induction step
        // try to find transition to recursively insert into subtrie,
        // or create a new transition if needed
        val (head, tail) = Pair(nameSuffix[0], nameSuffix.drop(1))
        return when (val subtrie = children[head].toOption()) {
            is Some ->
                // modify the subtrie, then return this
                subtrie.value.insert(tail, opt).map { this } // modify existing subtrie
            is None ->
                // create a new subtrie, insert it, and return this
                Trie()
                    .insert(tail, opt)
                    .map { subtrie -> children[head] = subtrie; this }
        }
    }
}