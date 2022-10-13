package com.riscure.langs.c

import arrow.core.*
import com.riscure.Digest
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories

interface Storage {
    /**
     * Returns a path that is stable and unique for the given inputs.
     */
    fun inputAddressed(prefix: String, vararg inputs: Digest, suffix: String = ""): File

    companion object {
        /**
         * An instance of Storage using temporary files.
         */
        @JvmStatic
        fun temporary(prefix: String = ""): Either<Throwable, FileStorage> = Either.catch {
            FileStorage(kotlin.io.path.createTempDirectory(prefix = prefix))
        }

        /**
         * An instance of Storage using a given directory.
         */
        @JvmStatic
        fun directory(path: Path): Either<Throwable, FileStorage> = Either.catch {
            path.createDirectories()
            FileStorage(path)
        }
    }

}

class FileStorage(val directory: Path): Storage {
    override fun inputAddressed(prefix: String, vararg inputs: Digest, suffix: String): File {
        val hash = Base64.getUrlEncoder()
            .encodeToString(Digest.combineAll(inputs.toList()).bytes)
            .take(15)

        return directory
            .resolve("${prefix}-${hash}${suffix}")
            .toFile()
    }
}

