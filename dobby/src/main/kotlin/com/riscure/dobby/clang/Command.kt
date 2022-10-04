package com.riscure.dobby.clang

typealias Options = List<Arg>

/**
 * The semantic model of Clang compilation commands
 */
data class Command(val optArgs: Options, val positionalArgs: List<String>) {
    /**
     * Return a command without [opt] or any of its aliases.
     */
    context(Spec)
    fun filter(opts: Set<OptionSpec>): Command {
        return this.copy(
            optArgs = optArgs.filter { (argSpec, _) -> !opts.any { blacklisted -> equal(argSpec, blacklisted) } }
        )
    }

    /**
     * Check if this command contains [opt] or any of its aliases.
     */
    context(Spec)
    fun contains(opts: Set<OptionSpec>): Boolean {
        return optArgs.any { (argSpec, _) -> opts.any { listed -> equal(listed, argSpec)} }
    }

    /**
     * Returns this command without [opt] or any of its aliases.
     */
    context(Spec)
    fun filter(opt: OptionSpec): Command = filter(setOf(opt))

    /**
     * Returns true iff this command contains [opt] or any of its aliases.
     */
    context(Spec)
    fun contains(opt: OptionSpec): Boolean = contains(setOf(opt))

    /**
     * Removes any alias of [arg.opt] and then adds [arg].
     */
    context(Spec)
    fun replace(arg: Arg): Command = filter(arg.opt).copy(optArgs = optArgs + arg)

    /**
     * Adds [arg] to the (rear of) the options of this command
     */
    fun plus(arg: Arg) = this.copy(optArgs = optArgs + arg)

    /**
     * Adds [positional] to the rear of the positional arguments of this command.
     */
    fun plus(positional: String) = this.copy(positionalArgs = positionalArgs + positional)

    companion object : arrow.typeclasses.Monoid<Command> {
        override fun empty(): Command = Command(listOf(), listOf())
        override fun Command.combine(b: Command): Command =
            Command(optArgs.plus(b.optArgs), positionalArgs.plus(b.positionalArgs))
    }
}

/**
 * Clang compilation optional arguments, with semantic info from the spec attached.
 */
data class Arg(val opt: OptionSpec, val values: List<String> = listOf()) {
    constructor(opt: OptionSpec, value: String):  this(opt, listOf(value))
}
