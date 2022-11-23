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
        if (lines.size < range.end.row) {
            return "Source range exceeds file boundaries".left()
        }

        val out = mutableListOf<String>()
        for ((i, line) in lines.withIndex()) {
            val lineno = i + 1

            // cut off the non-interesting bits
            if (lineno < range.begin.row) continue
            if (lineno > range.end.row) break

            // cut off the line segments we don't want
            val line2 = if (lineno == range.begin.row) line.substring(range.begin.col - 1) else line
            out.add(if (lineno == range.end.row) line2.substring(0, range.end.col - 1) else line2)
        }

        return out.joinToString(separator = "\n").right()
    }

}