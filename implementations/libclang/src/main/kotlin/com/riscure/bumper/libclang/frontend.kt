package com.riscure.bumper.libclang

import com.riscure.bumper.Frontend
import com.riscure.bumper.Storage
import com.riscure.bumper.preprocessor.impl.ClangPreprocessor
import java.nio.file.Path

/**
 * Create a pipeline based on clang tooling.
 */
fun frontend(clang: Path, cppStorage: Storage) = Frontend(
    ClangPreprocessor(clang),
    ClangParser(),
    cppStorage
)
