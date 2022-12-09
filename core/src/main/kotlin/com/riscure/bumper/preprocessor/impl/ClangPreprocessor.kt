package com.riscure.bumper.preprocessor.impl

import arrow.core.*
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.riscure.dobby.clang.Arg
import com.riscure.dobby.clang.Command
import com.riscure.dobby.clang.Options
import com.riscure.dobby.clang.Spec
import com.riscure.bumper.preprocessor.Preprocessor
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

/**
 * This implements the preprocessor interface by spawning a subprocess that calls clang -E.
 */
class ClangPreprocessor(private val clang: Path) : Preprocessor {
    private val log = LoggerFactory.getLogger(this.javaClass)

    override fun preprocess(main: File, opts: Options, out: File): Either<Throwable, Unit> = runBlocking {
        if (main.extension != "c") {
            // Yes...
            Preprocessor.Error(main, "Preprocessor expects input with '.c' file extension, got '$main'").left()
        }

        val cmd: Command =
             Command(opts, listOf(main.absolutePath))
                // replacing any existing output options
                .replace(Spec.clang11, Arg(Spec.clang11["o"], listOf(out.absolutePath)))
                // We add -O0, which really should be implied by -E, but it isn't.
                // It seems that in clang-16 it is.
                .plus(Arg(Spec.clang11["O0"]))
                .plus(Arg(Spec.clang11["E"]))

        log.info("Exec: $clang ${cmd.toArguments().joinToString(separator = " ")}")

        // Preprocess the input, writing to the output
        // FIXME do we need to worry about input/output encodings?
        val res = process(
            clang.toString(), *cmd.toArguments().toTypedArray(),

            // Both streams will be captured,
            // preserving their orders but mixing them in the given output.
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
        )

        // detect failures
        if (res.resultCode != 0) {
            Preprocessor.Error(main, res.output.joinToString(separator = "\n")).left()
        } else Unit.right()
    }
}
