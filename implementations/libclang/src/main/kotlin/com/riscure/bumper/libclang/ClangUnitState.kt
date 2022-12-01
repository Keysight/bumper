package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.DependencyAnalysis
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.UnitState
import com.riscure.bumper.pp.AstWriters
import com.riscure.bumper.pp.Extractor
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.nio.file.Path

/**
 * The Clang parser implementation now only analyzes upto expression/statements.
 * And then remembers the locations for those.
 */
typealias ClangTranslationUnit = TranslationUnit<CXCursor, CXCursor>
typealias ClangDeclaration     = Declaration<CXCursor, CXCursor>

class ClangUnitState(
    val tuid: TUID,
    val cxunit: CXTranslationUnit
) : UnitState<CXCursor, CXCursor> {

    private val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
    private val parsed by lazy {
        with(CursorParser(tuid)) {
            rootCursor
                .asTranslationUnit()
                .mapLeft { Throwable(it) }
        }
    }

    override fun close() = cxunit.close()

    override val ast by lazy { parsed.map { it.ast }}

    override val dependencies: DependencyAnalysis<CXCursor, CXCursor> by lazy {
        // this exception is part of the contract of the interface.
        val data = parsed.getOrHandle { error -> throw UnsupportedOperationException(error) }
        ClangDependencyAnalysis(tuid, data.elaboratedCursors)
    }

    // We could use libclang's pretty printing facilities here,
    // except that I've encountered corner cases where pretty printing returns "" incorrectly:
    // - https://github.com/llvm/llvm-project/issues/59155
    // So we fall back here on extracting lines from the source file instead.
    private val extractor = Extractor(tuid.main.toFile())
    val printer: AstWriters<CXCursor, CXCursor> by lazy {
        fun cursorPrinter(c: CXCursor) =
            c.getRange()
                .toEither { "Failed to get source range for expression." }
                .flatMap { extractor.extract(it) }

        AstWriters(::cursorPrinter, ::cursorPrinter)
    }
}

/**
 * In the context of a ClangUnitState we can convert some things
 * back to their libclang counterparts.
 */

/**
 * Returns the innermost cursor at [this] location.
 */
context(ClangUnitState)
fun Location.getCursor()  =
    clang.clang_getCursor(cxunit, cx())

context(ClangUnitState)
fun SourceRange.cx(): CXSourceRange =
    clang.clang_getRange(this.begin.cx(), this.end.cx())

context(ClangUnitState)
fun Location.cx(): CXSourceLocation =
    clang.clang_getLocation(cxunit, sourceFile.cx(), row, col)

// FIXME this can actually fail if the Path
// is not a path in the translation unit,
// which can be the case for those that we get from presumed locations, for example.
context(ClangUnitState)
fun Path.cx(): CXFile =
    clang.clang_getFile(cxunit, toString())
