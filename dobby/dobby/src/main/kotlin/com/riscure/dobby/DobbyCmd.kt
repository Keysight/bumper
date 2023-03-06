package com.riscure.dobby

import arrow.core.getOrElse
import arrow.core.getOrHandle
import com.riscure.dobby.clang.ClangParser
import com.riscure.dobby.clang.CompilationDb
import com.riscure.dobby.clang.OptionSpec
import com.riscure.dobby.clang.dropDuplicates
import picocli.CommandLine
import picocli.CommandLine.*
import java.nio.file.Path
import java.util.*
import java.util.concurrent.Callable
import kotlin.system.exitProcess
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.Ansi.*
import org.fusesource.jansi.Ansi.Color.*

private fun <T> error(msg: String): T  {
    println(ansi().fg(RED).a("ERROR: $msg").reset())
    exitProcess(1)
}

fun columnKey(str: String) = ansi().bold().a(str.padStart(15)).reset()

private fun printSpec(arg: OptionSpec) {
    val aliases = com.riscure.dobby.clang.Spec.clang11.aliases(arg).getOrElse { setOf() }

    System.err.println("""
        ${columnKey("key")}: ${arg.key}
        ${columnKey("prefixes")}: ${arg.prefixes.joinToString(separator = ", ")}
        ${columnKey("type")}: ${arg.type.describe()}
        ${columnKey("alias set")}: {${aliases.joinToString(", ") { it.appearance() }}}
    """.trimIndent())
}

@Command(
    name = "dobby",
    description = [ "Dobby Compilation Database Utility Elf" ],
    subcommands = [ DoValidate::class, DoInfo::class ],
)
class DobbyCmd: Callable<Int> {
    override fun call(): Int = 0

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AnsiConsole.systemInstall()
            exitProcess(CommandLine(DobbyCmd()).execute(*args))
        }
    }
}

@Command(name = "info", description = [ "Display the specification for a (partial) option" ])
class DoInfo: Callable<Int> {
    @Parameters(arity = "1", description = [ "The (partial) option string to parse" ])
    val option: Optional<String> = Optional.empty()

    override fun call(): Int {
        val input = option.orElseGet { error("No option given to parse") }
        ClangParser
            .parseClangArgument(input)
            .getOrHandle { msg -> error("Failed to parse, because: $msg") }
            .let {
                when (it) {
                    is ClangParser.Positional -> error("Expected option, got positional argument.")
                    is ClangParser.Partial    -> {
                        printSpec(it.arg.opt)
                        System.err.println(
                            "${columnKey("values")}: " +
                            "[${it.arg.values.joinToString(", ")}] " +
                            "(${it.expectValues} more expected)")
                    }
                    is ClangParser.Whole      -> {
                        printSpec(it.arg.opt)
                        System.err.println(
                            "${columnKey("values")}: " +
                            "[${it.arg.values.joinToString(", ")}]")
                    }
                }
            }

        return 0
    }
}

@Command(name = "validate", description = [ "Elaborate a Clang (11) Compilation Database" ])
class DoValidate: Callable<Int> {

    @Parameters(arity = "1", description = [ "The compilation database json file to validate" ])
    val cdb: Optional<Path> = Optional.empty()

    fun warning(msg: String) {
        msg
            .lines()
            .let { if (it.isEmpty()) listOf("") else it }
            .let { lines ->
                System.err.println("${ansi().fg(RED).a("<!> warning:").reset()} ${lines[0]}")
                if (lines.drop(1).isNotEmpty()) {
                    System.err.println(lines.joinToString("\n").prependIndent("  | "))
                }
            }
    }

    override fun call(): Int {
        val path = cdb.orElseGet { error("No compilation database given.") }
        val db = CompilationDb
            .read(path.toFile()) { entry, surprise ->
                warning("""
                    Failed to parse option '${ansi().bold().a(surprise).reset()}'
                    in entry for file '$entry'. Ignoring option.
                """.trimIndent())

                // recover
                true
            }
            .getOrHandle { exc -> error(exc.message!!) }

        val validated = db.dropDuplicates()

        // output warnings
        for (err in validated.messages) {
            warning(err)
        }

        // output validated result
        println(validated.value.toJSON("clang"))

        return 0
    }
}
