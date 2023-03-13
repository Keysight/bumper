package com.riscure.bumper.libclang

import arrow.core.Either
import arrow.core.flatMap
import com.riscure.bumper.Frontend
import com.riscure.bumper.Storage
import com.riscure.bumper.preprocessor.impl.ClangPreprocessor
import com.riscure.dobby.clang.Arg
import com.riscure.dobby.clang.CompilationDb
import com.riscure.dobby.clang.Spec
import org.bytedeco.llvm.clang.CXCursor
import java.io.File
import java.nio.file.Path

/**
 * Create a pipeline based on clang tooling.
 */
fun frontend(clang: Path, cppStorage: Storage) = ClangFrontend(clang, cppStorage)

class ClangFrontend(clang: Path, cppStorage: Storage):
    Frontend<CXCursor, CXCursor, ClangUnitState>(ClangPreprocessor(clang), ClangParser(), cppStorage) {
    fun process(main: File, cdb: CompilationDb): Either<Throwable, ClangUnitState> =
        cdb
            .get(main.toPath())
            .toEither { RuntimeException("No compile command for file '$main'") }
            .flatMap { entry ->
                process(
                    main,
                    entry.command
                        .replace(
                            Spec.clang11,
                            Arg(Spec.clang11["working_directory"], entry.workingDirectory.toString())
                        )
                        .optArgs
                )
            }
}
