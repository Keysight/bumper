package com.riscure.dobby.clang.parser

import com.riscure.dobby.clang.spec.*

import arrow.core.*

/**
 * The idea of the parser is as follows.
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

private typealias Result<T> = Either<String, T>

// Raw commands should be strings, because parsing is sensitive to how
// arguments are separated
private typealias RawCommand = String

// After recognizing an option, we might have to eat some expected arguments.
// the PartialArg class records the partial result and the expectation.
data class PartialArg(val opt: OptionSpec, val values: List<String>, val more: Int)

// After parsing, we will have a completely structured command
data class Arg(val opt: OptionSpec, val values: List<String>)
data class Command(val optArgs: List<Arg>, val positionalArgs: List<String>)

/* Parser */

private val clang11Trie: Trie by lazy {
    when (val trie = Trie.create(Spec.clang11.options())) {
        is Either.Left  -> throw RuntimeException("Failed to construct parse-trie for clang 11 option spec: ${trie.value}")
        is Either.Right -> trie.value
    }
}

fun parse(args: RawCommand): Result<Command> {
    return Either.Left("Fail")
}

fun parseClangArg(arg: String): Result<Match> {
    val match = clang11Trie.longestPrefix(arg)

    return if (match.isEmpty()) { "Unrecognized argument".left() } else { match.right() }
}

/* Mutable Trie datastructure */

private typealias Match = Set<Pair<OptionSpec,String>>
private class Trie(
    private var match: MutableSet<OptionSpec> = mutableSetOf(),
    private val children: MutableMap<Char, Trie> = mutableMapOf()
) {
    companion object {
        fun create(opts: Iterable<OptionSpec>): Either<String, Trie> =
            opts.foldM(Trie()) { acc, opt -> acc.insert(opt) }
    }

    fun longestPrefix(arg: String, longestSoFar: Match = setOf()): Match {
        // found new longest match?
        val longest: Match =  match
            .map { Pair(it, arg) }
            .toSet() // TODO ?? ugh
            .ifEmpty { longestSoFar }

        // when we're out of input then we are done
        if (arg.isEmpty()) {
            return longest
        }

        // if we still have input, we try to find a longer match
        // by searching for a transition to parse another char of input
        val edge = children.entries
            .find { (k, v) -> arg.startsWith(k) }
            .toOption()

        return when (edge) {
            // if we found a transition, traverse it
            is Some -> edge.value.value.longestPrefix(arg.drop(1), longest)
            // if we didn't, then we return the longest match so far
            else    -> longest
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

fun <A,R,E> Iterable<A>.foldM(acc: R, f: (R,A) -> Either<E,R>): Either<E,R> =
    this.iterator().foldM(acc, f)

fun <A,R,E> Iterator<A>.foldM(acc: R, f: (R,A) -> Either<E,R>): Either<E,R> =
    if (this.hasNext()) {
        f(acc, this.next()).flatMap { right -> this.foldM(right, f) }
    } else { acc.right() }