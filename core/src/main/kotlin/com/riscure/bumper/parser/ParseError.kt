package com.riscure.bumper.parser

import com.riscure.bumper.index.TUID
import com.riscure.dobby.clang.Options
import java.io.File

sealed class ParseError: Exception() {
    /**
     * Errors on the file-level, without a source location
     */
    data class FileFailed(
        val tuid: TUID,
        val options: Options,
        override val message: String
    ): ParseError()

    /**
     * Errors during parsing, may contain multiple diagnostics,
     * each with a source location.
     */
    data class ParseFailed(
        val tuid: TUID,
        val options: Options,
        val diagnostics: List<Diagnostic>
    ): ParseError() {
        override val message: String
            get() = "Failed to parse ${tuid.main}:\n${diagnostics.joinToString("\n") { it.format() }}"
    }

    /**
     * Internal errors indicative of a bug. User can't directly do much about these.
     */
    data class InternalError(
        val tuid: TUID,
        override val message: String,
        override val cause: Throwable? = null // JDK choice
    ): ParseError()

    data class PreprocFailed(val input: File, val reason: List<String>): ParseError() {
        constructor(input: File, reason: String): this(input, listOf(reason))

        override val message: String
            get() = "Failed to preprocess ${input}, saying:\n${reason.joinToString { it }}"

    }
}