package com.riscure.bumper.pp

import arrow.core.*
import com.riscure.bumper.ast.SourceRange
import com.riscure.bumper.ast.Declaration
import java.io.File
import java.nio.charset.Charset

/**
 * A class to extract source ranges from a file.
 */
class Extractor(val file: File, charset: Charset = Charset.defaultCharset()) {
    // We keep the whole thing buffered, because repeatedly
    // opening and scanning the file does not seem performant.
    private val lines = file.readLines(charset = charset)

    fun extract(range: SourceRange): Either<String, String> {
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
                    if (lineno == range.end.row) endColInclusive else (line.length - 1)
                )
            )
        }

        return out.joinToString(separator = "\n").right()
    }

}