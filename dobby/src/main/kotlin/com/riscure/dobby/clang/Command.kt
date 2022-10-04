package com.riscure.dobby.clang

typealias Options = List<Arg>

/**
 * The semantic model of Clang compilation commands
 */
data class Command(val optArgs: Options, val positionalArgs: List<String>) {
    /**
     * Return a command without [opt] or any of its aliases.
     * This requires a spec as context.
     */
    context(Spec)
    fun filter(opts: Set<OptionSpec>): Command {
        return this.copy(
            optArgs = optArgs.filter { (argSpec, _) -> !opts.any { blacklisted -> equal(argSpec, blacklisted) } }
        )
    }

    /**
     * Check if this command contains [opt] or any of its aliases.
     * This requires a spec as context.
     */
    context(Spec)
    fun contains(opts: Set<OptionSpec>): Boolean {
        return optArgs.any { (argSpec, _) -> opts.any { listed -> equal(listed, argSpec)} }
    }

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
