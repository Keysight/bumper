package com.riscure.bumper.libclang

import com.riscure.bumper.Frontend
import com.riscure.bumper.Storage
import com.riscure.bumper.preprocessor.impl.ClangPreprocessor
import org.bytedeco.llvm.clang.CXCursor
import java.nio.file.Path

/**
 * Create a pipeline based on clang tooling.
 */
fun frontend(clang: Path, cppStorage: Storage) = ClangFrontend(clang, cppStorage)

class ClangFrontend(clang: Path, cppStorage: Storage):
    Frontend<CXCursor, CXCursor, ClangUnitState>(ClangPreprocessor(clang), ClangParser(), cppStorage)
