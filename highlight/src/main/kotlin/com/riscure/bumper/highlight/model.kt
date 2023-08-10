/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import com.riscure.bumper.ast.Op
import java.nio.file.Path

data class Coverage(
    val count: Int
)

data class Position(
    val line: Int,
    val column: Int
)

enum class StrEncoding {

}

sealed interface Token {

    data class Operator(
        val position: Position,
        val op: Op
    ): Token

    data class Punctuation(
        val lexeme: String,
        val position: Position,
    ): Token

    data class StringLiteral(
        // val encoding: StrEncoding,
        val value: String,
        val position: Position
    ): Token

    data class Ws(
        val value: String,
        val position: Position
    ): Token
    object EOF: Token
}

data class LineSegment(
    val tokens: List<Token>,
    val coverage: Coverage,
)

data class Line(
    val segments: LineSegment
)

data class Source(
    /**
     * Some path (absolute/relative/symbolic) from where this source originates.
     */
    val path: Path,

    /**
     * The line-number of the first line in [lines] in [path].
     */
    val lineStart: Int,

    /**
     * The lines in this source blob.
     */
    val lines: List<Line>
)
