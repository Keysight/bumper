package com.riscure.dobby.clang

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
 */

private typealias Result<T> = Either<String,T>

object ClangParser {
    /**
     * This is the trie built from the parsed clang spec.
     */
    private val clang11Trie: Trie by lazy {
        when (val trie = Trie.create(Spec.clang11.options())) {
            is Either.Left -> throw RuntimeException("Failed to construct parse-trie for clang 11 option spec: ${trie.value}")
            is Either.Right -> trie.value
        }
    }

    private fun parseOptionArguments(input: String, spec: OptionSpec): Result<PartialArg> =
        when (spec.type) {
            OptionType.CommaJoined ->
                // for now we do not need to parse comma-joined values
                Whole(Arg(spec, listOf(input))).right()
            OptionType.Joined ->
                Whole(Arg(spec, listOf(input))).right()
            OptionType.JoinedAndSeparate ->
                Partial(Arg(spec, listOf(input)), 1).right()
            OptionType.JoinedOrSeparate ->
                if (input.isBlank()) Partial(Arg(spec), 1).right()
                else Whole(Arg(spec, listOf(input))).right()
            OptionType.Separate ->
                if (input.isBlank()) Partial(Arg(spec), 1).right()
                else "Unexpected joined value $input for option with separate argument ${spec.name}".left()
            OptionType.Toggle ->
                if (input.isBlank()) Whole(Arg(spec)).right()
                else "Unexpected value $input for toggle option ${spec.name}".left()
            is OptionType.MultiArg ->
                if (input.isBlank()) Partial(Arg(spec), spec.type.num).right()
                else "Unexpected joined value for option with separate argument ${spec.name}".left()
        }

    /**
     * Represents a possibly partially parsed option with its values or a positional argument.
     */
    sealed class PartialArg
    data class Positional(val value: String): PartialArg()
    data class Whole(val arg: Arg): PartialArg()
    data class Partial(val arg: Arg, val expectValues: Int): PartialArg()

    private fun parseClangArgument(arg: String): Result<PartialArg> {
        // The longest prefix parser finds all possible options
        // that match the input.
        val opts = clang11Trie.longestPrefix(arg)

        return if (opts.isEmpty()) {
            // No options for options
            tryPositional(arg)
        } else {
            // now we have to parse the option arguments
            for ((opt, rem) in opts) {
                when (val result = parseOptionArguments(rem, opt)) {
                    is Either.Left -> continue // try next
                    is Either.Right ->
                        // We assume than any match is a good match.
                        // We don't try to find out if the option arg
                        // has other successful parses
                        return result
                }
            }

            // Did not manage to successfully parse an option
            tryPositional(arg)
        }
    }

    @JvmStatic
    fun parseOption(head: String, vararg tail: String): Result<Arg> =
        parseClangArgument(head).flatMap { arg -> when (arg) {
            is Whole -> arg.arg.right()
            is Partial ->
                if (tail.size == arg.expectValues) {
                    "Expected exactly ${arg.expectValues} more arguments".left()
                } else {
                    arg.arg.copy(values = arg.arg.values.plus(tail)).right()
                }
            is Positional -> "Expected clang option, got $head".left()
        }}

    private fun tryPositional(input: String): Result<Positional> =
        if (input.startsWith("-")) "Unrecognized option $input".left()
        else Positional(input).right()

    /**
     * Parse a list of arguments, as specified by the compilation database reference, but
     * without the name of the executable at the head position.
     *
     * As per the specification, arguments are not shell-quoted.
     * If you have a shell-quoted line instead, you first have to parse it and evaluate
     * the quotes/escapes using com.riscure.dobby.shell.
     */
    @JvmStatic
    fun parseArguments(arguments: List<String>): Result<Command> = when (arguments.size) {
        0    -> Command.empty().right()
        else -> {
            // parse the head
            val other = arguments.drop(1)
            parseClangArgument(arguments[0])
                .flatMap { arg ->
                    when (arg) {
                        // depending on what we parse,
                        // we parse the tail differently
                        is Positional ->
                            parseArguments(other)
                                .map { tail -> tail.copy(positionalArgs = listOf(arg.value).plus(tail.positionalArgs)) }
                        is Whole ->
                            parseArguments(other)
                                .map { tail -> tail.copy(optArgs = listOf(arg.arg).plus(tail.optArgs)) }
                        is Partial ->
                            if (other.size < arg.expectValues) {
                                "Expected at least ${arg.expectValues} more arguments".left()
                            } else {
                                val a = arg.arg.copy(values = arg.arg.values.plus(other.take(arg.expectValues)))
                                parseArguments(other.drop(arg.expectValues))
                                    .map { tail -> tail.copy(optArgs = listOf(a).plus(tail.optArgs)) }
                            }
                    }
                }
        }
    }

    /**
     * Try to get a (possibly unstable) choice from the set of matching
     * options from a given prefix that should at least contain the option name.
     *
     * Example: optionFromPrefix(-O) may return spec.clang11["_SLASH_O"] or
     * spec.clang1["O"] depending on the orientation of the moon.
     */
    fun optionFromPrefix(prefix: String): Option<OptionSpec> =
        optionsFromPrefix(prefix).firstOrNone()


    /**
     * Get all possible matching option specs
     * for a given prefix that should at least contain the option name.
     *
     * This will not include aliases.
     * @see com.riscure.dobby.clang.Spec.aliasClosureOf
     */
    fun optionsFromPrefix(prefix: String): Set<OptionSpec> =
        clang11Trie.longestPrefix(prefix).map { it.first }.toSet()
}

/* Every spec that matches the input is paired with the remaining input */
private typealias Matches = Set<Pair<OptionSpec,String>>

/**
 * This trie represents a collection of prefixes with possible overlap
 * in such a way that we can efficiently find the longest matching prefix
 * given an input string.
 */
private class Trie(
    private var match: MutableSet<OptionSpec> = mutableSetOf(),
    private val children: MutableMap<Char, Trie> = mutableMapOf()
) {
    companion object {
        fun create(opts: Iterable<OptionSpec>): Result<Trie> =
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

    fun insert(opt: OptionSpec): Result<Trie> =
        opt.allAppearances().foldM(this) { acc, appearance -> acc.insert(appearance, opt) }

    private fun insertLeaf(opt: OptionSpec): Trie =
        this.match.add(opt).let { this }

    private fun insert(nameSuffix: String, opt: OptionSpec): Result<Trie> {
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
