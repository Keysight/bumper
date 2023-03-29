package com.riscure.bumper.index

import com.riscure.dobby.clang.CompilationDb
import kotlinx.serialization.Serializable
import java.nio.file.Path

/** Uniquely identify a translation unit */
data class TUID private constructor(
    /**
     * Translation units are identified primarily by the absolute path to the [main] source file.
     * This also makes TUIDs recocgnizable identifiers for end-users.
     *
     * This should really be the path to the unpreprocessed source.
     * The reason for this is that the TUID can potentially be persisted,
     * and the preprocessed source is usually only cached for uncertain amount of time.
     */
    val main: Path
) {
    companion object {
        @JvmStatic
        fun mk(cc: CompilationDb.Entry) = TUID(cc.resolvedMainSource)

        /**
         * @param main Absolute path to the main source file
         */
        @JvmStatic
        fun mk(main: Path): TUID {
            assert(main.isAbsolute)
            return TUID(main)
        }
    }
}