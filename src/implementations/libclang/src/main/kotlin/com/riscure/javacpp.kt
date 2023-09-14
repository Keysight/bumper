package com.riscure

import arrow.core.*
import org.bytedeco.javacpp.*
import org.bytedeco.llvm.clang.CXString
import org.bytedeco.llvm.global.clang

fun IntPointer.getOption() = if (isNull) None else get().some()

fun CXString.get(): String = clang.clang_getCString(this).string

fun CXString.getOption(): Option<String> =
    this.some()
        .filter { it.isNull() }
        .map { clang.clang_getCString(this) }
        .filter { it.isNull }
        .map { it.string }

fun Int.toBool() = this != 0