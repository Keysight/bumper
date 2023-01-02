package com.riscure.bumper.index

import kotlinx.serialization.Serializable
import java.nio.file.Path

/** Uniquely identify a translation unit */
@Serializable
data class TUID(
    /** Translation units are identified by their main (preprocessed) file */
    val main: Path
)