package com.riscure.langs.c.pp

import arrow.core.*
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.Declaration
import java.io.File
import java.nio.charset.Charset

/**
 * A class to extract source ranges from a file.
 */
class Extractor(val file: File, charset: Charset = Charset.defaultCharset()) {
    // We keep the whole thing buffered, because repeatedly
    // opening and scanning the file does not seem performant.
    private val lines = file.readLines(charset = charset)

    fun sourceOf(range: SourceRange): Either<Throwable, String> {
        if (lines.size < range.end.row) {
            return Throwable("Source range exceeds file boundaries").left()
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

    /**
     * Returns the right-hand side of a top-level representing a definition if any, or "" otherwise.
     * This can fail if the top-level entity has no source location, or if the location is somehow not valid,
     * or if we otherwise fail to extract the rhs from the source.
     */
    fun rhsOf(tl: Declaration): Either<Throwable, String> = when (tl) {
        is Declaration.Fun ->
            if (!tl.isDefinition)
                Either.Right("")
            else
                tl.meta.location
                    .toEither { Throwable("Cannot extract source for top-level entity without location") }
                    .flatMap { sourceOf(it) }
                    .flatMap { functionBodyOf(it) }
        is Declaration.Composite -> "".right() // TODO
        is Declaration.EnumDef -> "".right() // TODO
        is Declaration.Typedef -> "".right()
        is Declaration.Var ->
            if (!tl.isDefinition)
                Either.Right("")
            else
                tl.meta.location
                    .toEither { Throwable("Cannot extract source for top-level entity without location") }
                    .flatMap { sourceOf(it) }
                    .flatMap { varBodyOf(it) }
    }

    private fun functionBodyOf(src: String): Either<Throwable, String> =
        Regex("[^{]*\\{(.*)\\}[^}]*", RegexOption.DOT_MATCHES_ALL).find(src)
            .toOption()
            .toEither { Throwable("Failed to extract function body") }
            .map {
                it.groupValues[1]
            }

    private fun varBodyOf(src: String): Either<Throwable, String> =
        Regex("[^=]*=(.*)$", RegexOption.DOT_MATCHES_ALL).find(src)
            .toOption()
            .toEither { Throwable("Failed to extract right-hand side of variable definition.") }
            .map {
                it.groupValues[1]
            }
}