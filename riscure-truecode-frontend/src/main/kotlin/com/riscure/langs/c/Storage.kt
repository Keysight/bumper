package com.riscure.langs.c

import arrow.core.*
import java.io.File

interface Storage {
    /**
     * Returns a path that is stable and unique for the given inputs.
     */
    fun inputAddressed(prefix: String, vararg inputs: Any, suffix: String = ""): File

    companion object {
        /**
         * An instance of Storage using temporary files.
         */
        fun temporary(prefix: String = ""): Either<Throwable, Storage> = Either.catch {
            val directory = kotlin.io.path.createTempDirectory(prefix = prefix)

            object : Storage {
                override fun inputAddressed(prefix: String, vararg inputs: Any, suffix: String): File =
                    directory
                        .resolve("${prefix}_${inputs.hashCode()}${suffix}")
                        .toFile()
            }
        }
    }
}