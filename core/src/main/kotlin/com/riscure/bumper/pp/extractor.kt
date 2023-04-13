package com.riscure.bumper.pp

import arrow.core.*
import com.riscure.bumper.ast.SourceRange
import java.nio.charset.Charset
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * An interface to get source ranges as strings.
 */
interface Extractor {
    fun extract(range: SourceRange): Either<String, String>
}

/**
 * An extractor based on reading from a buffered file.
 */
class SourceExtractor(val source: Path, charset: Charset = Charset.defaultCharset()): Extractor {
    // We keep the whole thing buffered, because repeatedly
    // opening and scanning the file does not seem performant.
    private val lines = source.readLines(charset = charset)

    override fun extract(range: SourceRange): Either<String, String> {
        val endColInclusive = range.end.col - 1

        if (lines.size < range.end.row ||
            lines.size == range.end.row && lines.last().length < endColInclusive) {
            return "Source range exceeds file boundaries".left()
        }

        val out = mutableListOf<String>()
        for ((i, line) in lines.withIndex()) {
            val lineno = i + 1

            // cut off the non-interesting bits
            if (lineno < range.begin.row) continue
            if (lineno > range.end.row) break

            // cut off the line segments we don't want
            out.add(
                line.substring(
                    if (lineno == range.begin.row) (range.begin.col - 1) else 0,
                    if (lineno == range.end.row) endColInclusive else line.length
                )
            )
        }

        return out.joinToString(separator = "\n").right()
    }

}