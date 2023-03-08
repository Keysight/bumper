package com.riscure.bumper.ast

import arrow.core.compareTo
import java.nio.file.Path
import kotlin.io.path.extension

/** The type of raw identifiers */
typealias Ident = String

typealias TypeRef  = TLID
typealias Ref = TLID

/**
 * This is a comparator for ranges of declarations in the same unit.
 * That means that the ranges that are partially overlapping are considered equal,
 * but nested ones have an order.
 *
 * One source range is less than another if it appears logically before the other.
 * For nested ranges, this means that the innermost is less than the outermost.
 */
object dependencyOrder: Comparator<SourceRange> {
    override fun compare(r1: SourceRange, r2: SourceRange): Int = when {
        r1.end   < r2.begin -> -1                    // r2 disjoint after r1
        r1.begin > r2.end   -> 1                     // r1 disjoint after r2
        r1.begin > r2.begin && r1.end < r2.end -> -1 // r1 nested in r2
        r2.begin > r1.begin && r2.end < r1.end -> 1  // r2 nested in r1
        else                -> 0                     // not disjoint, or exactly overlapping
    }
}

/** A source location */
data class Location(
    val sourceFile: Path,
    /** The line number, with first line being 1 */
    val row: Int,
    /** The column number, with first column being 1 */
    val col: Int
) {
    /**
     * compares the row and col of this to [other], ignoring the sourceFile.
     */
    operator fun compareTo(other: Location): Int =
        Pair(row, col).compareTo(Pair(other.row, other.col))

    fun isHeader(): Boolean = sourceFile.extension == "h"

    override fun toString(): String = "$sourceFile ($row:$col)"
}

/** A source range with a begin and end location. */
data class SourceRange(
    /** begin location of the source range (inclusive) */
    val begin: Location,
    /** end location of the source range (inclusive) */
    val end: Location
) {
    /**
     * Returns true iff this range fully encloses [other]
     */
    fun encloses(other: SourceRange): Boolean =
        file == other.file && begin <= other.begin && end >= other.end

    fun encloses(other: Location): Boolean =
        file == other.sourceFile && begin <= other && end >= other

    /** Gets the sourceFile of [begin], which is assumed to match [end]'s */
    val file get() = begin.sourceFile
}
