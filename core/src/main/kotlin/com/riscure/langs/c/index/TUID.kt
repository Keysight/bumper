package com.riscure.langs.c.index

import java.nio.file.Path

/* Uniquely identify a translation unit */
data class TUID(
    /* Translation units are identified by their main (preprocessed) file */
    val main: Path
)