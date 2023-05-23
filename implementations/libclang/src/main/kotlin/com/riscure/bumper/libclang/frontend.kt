package com.riscure.bumper.libclang

import com.riscure.bumper.Frontend
import com.riscure.bumper.Storage
import com.riscure.bumper.ast.Stdlib
import com.riscure.bumper.preprocessor.impl.ClangPreprocessor
import org.bytedeco.llvm.clang.CXCursor
import java.nio.file.Path

/**
 * Create a pipeline based on clang tooling.
 */
fun frontend(clang: Path, cppStorage: Storage) = ClangFrontend(clang, cppStorage)

/**
 * A clang frontend implementation for supported x64 host platforms.
 */
class ClangFrontend(clang: Path, cppStorage: Storage):
    Frontend<CXCursor, CXCursor, ClangUnitState>(
        Stdlib.x64host,
        ClangPreprocessor(clang),
        ClangParser(),
        cppStorage
    )
