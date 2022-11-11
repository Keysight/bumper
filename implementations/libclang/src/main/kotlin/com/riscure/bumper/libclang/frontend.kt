package com.riscure.bumper.libclang

import com.riscure.langs.c.Frontend
import com.riscure.langs.c.Storage
import com.riscure.langs.c.preprocessor.clang.ClangPreprocessor
import java.nio.file.Path

/**
 * Create a pipeline based on clang tooling.
 */
fun frontend(clang: Path, cppStorage: Storage) = Frontend(
    ClangPreprocessor(clang),
    ClangParser(),
    cppStorage
)
