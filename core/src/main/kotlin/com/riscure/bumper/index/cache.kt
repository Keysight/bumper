package com.riscure.bumper.index

import com.riscure.Digest
import java.nio.file.Path

data class Artifact(
    val inode       : Int,
    val mtimeSeconds: Long,
    val mtimeNano   : Long,
    val dependencies: Set<Path>,
    val digest      : Digest,
    val path        : Path
)

/**
 * The cache is a data-structure that tracks artifacts that exist on disk.
 * Each artifact has information attached to it to be able to check if it is up-to-date.
 *
 * For example. Let's say you have previously indexed a C translation unit and you
 * need to know if the index is still up-to-date. The function that performs the
 * indexing is a pure function on the AST of the unit.
 * Hence, in order to know if the index is up-to-date, we have to
 * check if the AST that we would obtain from the parser is the same as the one that we
 * used last time.
 *
 * The AST that you will obtain depends not only on the contents of this file,
 * but also on the contents of all its #include'd dependencies, on the defined macros,
 * and possibly on the compiler used to parse the file, and flags passed to the compiler
 * that influence builtin definitions.
 *
 */
data class Cache(
    val artifacts: Map<Path, Artifact>
) {
}