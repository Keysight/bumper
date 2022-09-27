package com.riscure.dobby.clang.parser

import com.riscure.dobby.clang.spec.*

import arrow.core.*
import stdlibpp.*

/* Clang command parser.
 *
 * It has three big states, one for parsing a command consisting of options,
 * one for parsing an option, and one for parsing an option's arguments.
 * They are implemented by functions that interleave each-other.
 *
 * The idea of the parser is as follows:
 *
 * Given a list of arguments a:as, the parser will try to recognize
 * a valid option (with possible attached values) in the head a.
 * If we succeed, we can determine from the spec how many arguments
 * we still expect in as. We peel the arguments off the list, and
 * attempt to parse the next option.
 *
 * At some point we will fail to parse an option where one is expected.
 * At that point, we conclude we have reached the positional arguments.
 */

private typealias Result<T> = Either<String, Pair<T, String>>
data class Arg(val opt: OptionSpec, val values: Option<String> = None)
data class Command(val optArgs: List<Arg>, val positionalArgs: List<String>)

object Parser {
    private val clang11Trie: Trie by lazy {
        when (val trie = Trie.create(Spec.clang11.options())) {
            is Either.Left -> throw RuntimeException("Failed to construct parse-trie for clang 11 option spec: ${trie.value}")
            is Either.Right -> trie.value
        }
    }

    fun maybeSpace(input: String): Result<Unit> = input
        .dropWhile { it.isWhitespace() }
        .let { Pair(Unit, it) }
        .right()

    fun space(input: String): Result<Unit> {
        val (ws, remains) = input.partition { it.isWhitespace() }
        return if (ws.isEmpty()) {
            "Expected whitespace, got '$input'".left()
        } else {
            Pair(Unit, remains).right()
        }
    }

    fun untilUnescapedWhitespace(input: String): Result<String> = TODO()

    private fun parseOptionArguments(input: String, spec: OptionSpec): Result<Arg> = TODO()
//    {
//        when (spec.type) {
//            OptionType.CommaJoined ->
//                maybeSpace(input)
//                    .flatMap { ignored -> untilUnescapedWhitespace(ignored.second) }
//            OptionType.Joined -> untilUnescapedWhitespace(input)
//            OptionType.JoinedAndSeparate -> TODO()
//            OptionType.JoinedOrSeparate ->
//                maybeSpace(input)
//                    .flatMap { ignored -> untilUnescapedWhitespace(ignored.second) }
//            OptionType.Separate ->
//                space(input)
//                    .flatMap { ignored -> untilUnescapedWhitespace(ignored.second) }
//            OptionType.Toggle ->
//                space(input)
//                    .map { Pair(Arg(spec), input).right() }
//            is OptionType.MultiArg -> TODO()
//        }
//    }

    fun parseClangOption(input: String): Result<Arg> {
        // The longest prefix parser finds all possible options
        // that match the input.
        val (opts, rem) = clang11Trie.longestPrefix(input)

        return if (opts.isEmpty()) {
            "Failed to parse option from input '${input}'".left()
        } else {
            // now we have to parse the option arguments
            for (opt in opts) {
                when (val arg = parseOptionArguments(rem, opt)) {
                    is Either.Left -> continue // try next
                    is Either.Right ->
                        // We assume than any match is a good match.
                        // We don't try to find out if the option arg
                        // has other successful parses
                        return arg
                }
            }

            // No matches
            return "Failed to parse option argument from input '${rem}'".left()
        }
    }
}

/* Mutable Trie datastructure */
private typealias Matches = Set<OptionSpec>
private class Trie(
    private var match: MutableSet<OptionSpec> = mutableSetOf(),
    private val children: MutableMap<Char, Trie> = mutableMapOf()
) {
    companion object {
        fun create(opts: Iterable<OptionSpec>): Either<String, Trie> =
            opts.foldM(Trie()) { acc, opt -> acc.insert(opt) }
    }

    fun longestPrefix(input: String, longestSoFar: Matches = setOf()): Pair<Matches, String> {
        // found new longest match?
        val longest: Matches = match
            .ifEmpty { longestSoFar }

        // when we're out of input then we are done
        if (input.isEmpty()) {
            return Pair(longest, "")
        }

        // if we still have input, we try to find a longer match
        // by searching for a transition to parse another char of input
        val edge = children.entries
            .find { (k, v) -> input.startsWith(k) }
            .toOption()

        val remainingInput = input.drop(1)
        return when (edge) {
            // If we found a transition, traverse it
            // There cannot be multiple, because edges have disjoint labels.
            is Some -> edge.value.value.longestPrefix(remainingInput, longest)
            // if we didn't, then we return the longest match so far
            else    -> Pair(longest, remainingInput)
        }
    }

    fun insert(opt: OptionSpec): Either<String,Trie> =
        opt.allAppearances().foldM(this) { acc, appearance -> acc.insert(appearance, opt) }

    private fun insertLeaf(opt: OptionSpec): Trie =
        this.match.add(opt).let { this }

    private fun insert(nameSuffix: String, opt: OptionSpec): Either<String,Trie> {
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
