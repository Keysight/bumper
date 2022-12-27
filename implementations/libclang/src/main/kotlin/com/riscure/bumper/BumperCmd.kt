package com.riscure.bumper

import arrow.core.*
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.libclang.ClangParser
import com.riscure.bumper.pp.Printer
import picocli.CommandLine
import picocli.CommandLine.*
import picocli.CommandLine.Option
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import kotlin.collections.flatMap
import kotlin.system.exitProcess

@Command(
    name = "bumper",
    description = [ "Bumper C Frontend utilities" ],
    subcommands = [ DoElab::class, DoDeps::class ],
)
class BumperCmd: Callable<Int> {
    override fun call(): Int = 0
}

@Command(name = "elab", description = [ "Elaborate a C file" ])
class DoElab: Callable<Int> {

    @Parameters val cfile: Optional<Path> = Optional.empty()

    override fun call(): Int {
        val file = cfile.orElseGet { throw RuntimeException("Not input file given.") }

        // parse the input
        val parser = ClangParser()
        val unit = parser
            .parse(file.toFile())
            .getOrHandle { throw it }

        // pretty-print it again
        val writer = System.out.writer()
        unit.printer
            .print(unit.ast)
            .getOrHandle { throw RuntimeException(it) }
            .write(Printer.of(writer))
        writer.flush()

        return 0
    }
}

@Command(name = "deps", description = [ "Produce the dependency graph between symbols" ])
class DoDeps: Callable<Int> {

    @Parameters val cfile: Optional<Path> = Optional.empty()

    @Option(names = [ "-u" ]) var keepUnconnected: Boolean = false
    @Option(names = [ "-r", "--root" ]) var root: Optional<String> = Optional.empty()

    override fun call(): Int {
        val file = cfile.orElseGet { throw RuntimeException("Not input file given.") }

        // parse the input
        val parser = ClangParser()
        val unit = parser
            .parse(file.toFile())
            .getOrHandle { throw it }

        // construct the graph
        val deps = unit.dependencies
            .getOrHandle { throw RuntimeException(it) }
            .let { graph ->
                if (root.isPresent) {
                    // find the symbol matching the given name
                    val s = graph.dependencies.keys
                        .find { s -> s.name == root.get() }
                        .toOption()
                        .getOrElse { throw RuntimeException("No symbol found with name ${root.get()}.") }

                    // filter the graph
                    graph.reachable(setOf(s))
                } else graph
            }
            .dependencies

        fun nodeId(s: Symbol) = "${s.name}"
        fun nodeDisplay(s: Symbol) = "${s.kind} ${s.name}"

        // produce the DOT declarations
        val nodes = deps.flatMap { (k, ds) ->
            // filter out unconnected nodes.
            if (keepUnconnected || ds.size > 0)
                listOf("${nodeId(k)} [label=\"${nodeDisplay(k)}\"];")
            else listOf()
        }

        val edges = deps
            .flatMap { (k, deps) -> deps.map { d -> "${nodeId(k)} -> ${nodeId(d)};" } }

        // Output the graph
        println("""
            digraph dependencies {
                ${nodes.joinToString("\n")}
                ${edges.joinToString("\n")}
            }
        """.trimIndent())
        System.out.flush()

        return 0
    }
}

fun main(args: Array<String>) {
    exitProcess(CommandLine(BumperCmd()).execute(*args))
}