/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import arrow.core.Option
import arrow.core.compareTo
import arrow.core.none
import java.util.*

data class Segment(
    val tokens: List<Token>,
    val coverage: Option<Coverage> = none()
)

data class Coverage(
    val count: Int
)

/**
 * From the coverage instrumentation we get a coverage model
 * consisting of a list of "segments", each indicating the start
 * of a section with uniform coverage.
 *
 * These markers are only really useful when you have a list of them,
 * because there is no 'end' marker for a section, except the subsequent one in the list.
 */
data class SegmentMarker(
    val line: Int,
    val column: Int,
    val count: Option<Int>,
)

typealias Segments = List<SegmentMarker>

fun annotate(tokenizer: Tokens, segments: Segments): List<Segment> = annotate(
    LinkedList<Token>().apply { tokenizer.forEachRemaining { add(it) } },
    segments
)

/**
 * This function annotates the [tokens] token stream with coverage data
 * represented by a sequential list [segments] of [SegmentMarker].
 *
 * The output is a list of sequential grouped and annotated tokens represented by a [Segment].
 */
fun annotate(
    tokens: Queue<Token>,
    segments: Segments,
): List<Segment> {
    val result = mutableListOf<Segment>()
    var currentCov = none<Coverage>()
    var segment = mutableListOf<Token>()

    segments.forEach { nextMarker ->
        val end = nextMarker.run { Pair(line, column) }

        // Walk over the tokens, keeping track of the cursor position.
        // As we look at the next token, we check if it falls into a new segment,
        // splitting the segments.
        while (tokens.isNotEmpty()) {
            val tok = tokens.peek()
            val cursor = tok.position.run { Pair(line, column) }

            // check if the new segment starts here
            if (end <= cursor) {
                // finish the previous segment
                result.add(Segment(segment, currentCov))

                // continue with the next, new segment,
                // with the coverage from the marker.
                segment = mutableListOf()
                currentCov = nextMarker.count.map { Coverage(it) }
                break
            } else {
                // drop this token from the queue
                tokens.remove()

                // add to the current segment
                segment.add(tok)
            }
        }
    }

    return result
}