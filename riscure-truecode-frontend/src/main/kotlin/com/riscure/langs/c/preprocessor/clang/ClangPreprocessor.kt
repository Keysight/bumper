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
        val cliArgs: Array<String> = arrayOf(
            "-O0", "-nostdinc",
            "-idirafter", "/nix/store/dwsi3wsqpqm0hpzdm9fsxc7q732p9xwi-glibc-2.34-210-dev/include",
            "-idirafter", "/nix/store/f5k2sz6bm05cxg8hnlmij7pc5f4i1zaj-clang-11.1.0-lib/lib/clang/11.1.0/include/",
            "-E", main.absolutePath, "-o", out.absolutePath)

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