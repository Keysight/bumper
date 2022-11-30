package com.riscure.dobby.clang

import arrow.core.*
import com.riscure.dobby.shell.Arg as ShellArg

typealias Options = List<Arg>

/**
 * The semantic model of Clang compilation commands
 */
data class Command(val optArgs: Options, val positionalArgs: List<String>) {
    /**
     * Return a command without [opt] or any of its aliases.
     */
    fun filter(spec: Spec, opts: Collection<OptionSpec>): Command {
        return this.copy(
            optArgs = optArgs.filter { (argSpec, _) ->
                !opts.any { blacklisted -> spec.equal(argSpec, blacklisted) }
            }
        )
    }

    /**
     * Check if this command contains [opt] or any of its aliases.
     */
    fun contains(spec: Spec, opts: Set<OptionSpec>): Boolean {
        return optArgs.any { (argSpec, _) -> opts.any { listed -> spec.equal(listed, argSpec)} }
    }

    /**
     * Returns this command without [opt] or any of its aliases.
     */
    fun filter(spec: Spec, opt: OptionSpec): Command = filter(spec, setOf(opt))

    /**
     * Returns true iff this command contains [opt] or any of its aliases.
     */
    fun contains(spec: Spec, opt: OptionSpec): Boolean = contains(spec, setOf(opt))

    /**
     * Removes any alias of [arg.opt] and then adds [arg].
     */
    fun replace(spec: Spec, arg: Arg): Command = filter(spec, arg.opt) + arg

    /**
     * Adds [arg] to the (rear of) the options of this command
     */
    operator fun plus(arg: Arg) = this.copy(optArgs = optArgs + arg)

    /**
     * Adds [arg] to the (rear of) the options of this command
     */
    operator fun plus(args: List<Arg>) = this.copy(optArgs = optArgs + args)

    /**
     * Adds [positional] to the rear of the positional arguments of this command.
     */
    operator fun plus(positional: String) = this.copy(positionalArgs = positionalArgs + positional)

    fun toArguments(): List<String> =
        optArgs.map { it.shellify().toString() } + positionalArgs.map { ShellArg.quote(it).toString() }

    companion object : arrow.typeclasses.Monoid<Command> {
        @JvmStatic
        override fun empty(): Command = Command(listOf(), listOf())
        override fun Command.combine(b: Command): Command =
            Command(optArgs.plus(b.optArgs), positionalArgs.plus(b.positionalArgs))

        @JvmStatic
        fun reads(arguments: List<String>) = ClangParser.parseArguments(arguments)
    }
}

/**
 * Clang compilation optional arguments, with semantic info from the spec attached.
 */
data class Arg(val opt: OptionSpec, val values: List<String>) {
    constructor(opt: OptionSpec, value: String):  this(opt, listOf(value))
    constructor(opt: OptionSpec):  this(opt, listOf())

    /**
     * Join arguments according to the option specification.
     */
    fun shellify() = when(opt.type) {
        OptionType.Joined            -> ShellArg.quote(opt.appearance(), *values.toTypedArray())
        OptionType.CommaJoined       -> ShellArg.quote(opt.appearance(), values.joinToString(separator = ",") { it })
        OptionType.JoinedAndSeparate -> ShellArg.quote(opt.appearance() + values[0], *values.drop(1).toTypedArray())
        OptionType.JoinedOrSeparate  -> ShellArg.quote(opt.appearance(), *values.toTypedArray()) // separate
        OptionType.Separate          -> ShellArg.quote(opt.appearance(), *values.toTypedArray())
        OptionType.Toggle            -> ShellArg.quote(opt.appearance())
        is OptionType.MultiArg       -> ShellArg.quote(opt.appearance(), *values.toTypedArray())
    }

    companion object {
        /**
         * Read a single option argument with a possible [tail] of separated values.
         */
        @JvmStatic fun reads(head: String, vararg tail: String) = ClangParser.parseOption(head, *tail)
        @JvmStatic fun readMany(args: List<List<String>>): Either<String, List<Arg>> = args
            .flatMap { parts ->
                if (parts.isEmpty()) {
                    listOf()
                } else {
                    listOf(ClangParser.parseOption(parts[0], *parts.drop(1).toTypedArray()))
                }
            }
            .sequence()
    }
}
