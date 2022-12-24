package com.riscure.bumper

import arrow.core.getOrHandle
import com.riscure.bumper.libclang.ClangParser
import com.riscure.bumper.pp.Printer
import picocli.CommandLine
import picocli.CommandLine.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    name = "bumper",
    description = [ "Bumper C Frontend utilities" ],
    subcommands = [ DoElab::class ],
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

fun main(args: Array<String>) {
    exitProcess(CommandLine(BumperCmd()).execute(*args))
}