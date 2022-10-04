package com.riscure.langs.c.preprocessor.clang

import arrow.core.*
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.riscure.dobby.clang.Options
import com.riscure.langs.c.preprocessor.Preprocessor
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

/**
 * This implements the preprocessor interface by spawning a subprocess that calls clang -E.
 */
class ClangPreprocessor(private val clang: Path) : Preprocessor {
    override fun preprocess(main: File, opts: Options, out: File): Either<Throwable, Unit> = runBlocking {
        if (main.extension != "c") {
            // Yes...
            Preprocessor.Error(main, "Preprocessor expects input with '.c' file extension, got '$main'").left()
        }

        // TODO
        // We add -O0, which really should be implied by -E, but it isn't.
        // It seems that in clang-16 it is.
        val cliArgs: Array<String> = arrayOf("-O0", "-E", main.absolutePath, "-o", out.absolutePath)

        // Preprocess the input, writing to the output
        // FIXME do we need to worry about input/output encodings?
        val res = process(
            clang.toString(), *cliArgs,

            // Both streams will be captured,
            // preserving their orders but mixing them in the given output.
            stdout = Redirect.CAPTURE,
            stderr = Redirect.CAPTURE,
        )

        // detect failures
        if (res.resultCode != 0) {
            Preprocessor.Error(main, res.output).left()
        } else Unit.right()
    }
}