package com.riscure.dobby.clang

/**
 * The semantic model of Clang compilation commands
 */
data class Command(val optArgs: List<Arg>, val positionalArgs: List<String>) {
    /**
     * Return a command without [opt] or any of its aliases.
     * This requires a spec as context.
     */
    context(Spec)
    fun filter(opts: Set<OptionSpec>): Command {
        val aliases = opts.aliasClosure()
        return this.copy(
            optArgs = optArgs.filter { (argSpec, _) -> !aliases.contains(argSpec) }
        )
    }

    /**
     * Check if this command contains [opt] or any of its aliases.
     * This requires a spec as context.
     */
    context(Spec)
    fun contains(opts: Set<OptionSpec>): Boolean {
        val aliases = opts.aliasClosure()
        return optArgs.any { (argSpec, _) -> aliases.contains(argSpec) }
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
data class Arg(val opt: OptionSpec, val values: List<String> = listOf())
