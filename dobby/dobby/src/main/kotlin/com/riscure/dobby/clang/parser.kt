package com.riscure.dobby.clang

import arrow.core.*
import com.riscure.dobby.Trie
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
private typealias RecoverCallback = (surprise: String) -> Boolean

object RecoveryCallbacks {
    val always: RecoverCallback = { true }
    val strict: RecoverCallback = { false }
}

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

    /**
     * Try to parse exactly one option with its specified values.
     * Fails on positional arguments and unrecognized options.
     */
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

    /**
     * Parse a list of options, as specified by the compilation database reference but without the
     * executable as the first argument and fail if there are any positional arguments.
     *
     * @param options   As per the specification, arguments are not shell-quoted.
     *                  If you have a shell-quoted line instead, you first have to parse it and evaluate
     *                  the quotes/escapes using com.riscure.dobby.shell.
     * @param recover   The function calls back when it encounters a string that looks like an option
     *                  but it is not in the spec. The boolean returned should indicate if we can just drop it.
     */
    @JvmStatic
    fun parseOptions(options: List<String>, recover: RecoverCallback = { false }): Result<Options> =
        parseArguments(options, recover)
            .flatMap { cmd ->
                if (cmd.positionalArgs.isNotEmpty())
                    "Expected only options, but found positional arguments: ${cmd.positionalArgs}".left()
                else
                    cmd.optArgs.right()
            }

    /**
     * Same as [parseOptions] except that we assume that the options are valid (usually
     * because they are statically sufficiently known) and expect a successful parse.
     * A [RuntimeException] is thrown when the parse is not valid.
     */
    @JvmStatic
    fun parseValidOptions(vararg options: String, recover: RecoverCallback = { false }) =
        parseOptions(options.toList(), recover).getOrHandle { throw RuntimeException(it) }

    /**
     * Parse a list of arguments, as specified by the compilation database reference but without the
     * executable as the first argument.
     *
     * @param arguments As per the specification, arguments are not shell-quoted.
     *                  If you have a shell-quoted line instead, you first have to parse it and evaluate
     *                  the quotes/escapes using com.riscure.dobby.shell.
     * @param recover   The function calls back when it encounters a string that looks like an option
     *                  but it is not in the spec. The boolean returned should indicate if we can just drop it.
     */
    @JvmStatic
    fun parseArguments(arguments: List<String>, recover: RecoverCallback = { false }): Result<Command> =
        when (arguments.size) {
            0    -> Command.empty().right()
            else -> {
                // parse the head
                val other  = arguments.drop(1)
                val parsed = parseClangArgument(arguments[0])

                when (parsed) {
                    // Report the surprising argument and see if the caller wants us to continue
                    is Either.Left  -> if (recover(arguments[0])) parseArguments(other) else parsed
                    is Either.Right -> parsed.value.let { arg ->
                        when (arg) {
                            // depending on what we parse,
                            // we parse the tail differently
                            is Positional ->
                                parseArguments(other, recover)
                                    .map { tail -> tail.copy(positionalArgs = listOf(arg.value).plus(tail.positionalArgs)) }
                            is Whole      ->
                                parseArguments(other, recover)
                                    .map { tail -> tail.copy(optArgs = listOf(arg.arg).plus(tail.optArgs)) }
                            is Partial    ->
                                if (other.size < arg.expectValues) {
                                    ( "Expected at least ${arg.expectValues} more arguments " +
                                      "for option ${arg.arg.opt.appearance()}"
                                    ).left()
                                } else {
                                    val a = arg.arg.copy(values = arg.arg.values.plus(other.take(arg.expectValues)))
                                    parseArguments(other.drop(arg.expectValues), recover)
                                        .map { tail -> tail.copy(optArgs = listOf(a).plus(tail.optArgs)) }
                                }
                        }
                    }
                }
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
                else Whole(Arg(spec, listOf(input.trimStart()))).right()
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

    /**
     * Longest prefix parser for options.
     * This is primarily for internal use. You probably don't want to use this function.
     */
    fun parseClangArgument(arg: String): Result<PartialArg> {
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

    private fun tryPositional(input: String): Result<Positional> =
        if (input.startsWith("-")) "Unrecognized option $input".left()
        else Positional(input).right()

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

