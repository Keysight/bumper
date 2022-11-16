package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.DependencyAnalysis
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.UnitState
import com.riscure.bumper.pp.AstWriters
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.nio.file.Path

/**
 * The Clang parser implementation now only analyzes upto expression/statements.
 * And then remembers the cursor for those. These cursors are invalidated
 * when the unit closes, so that would be a good time to upcast the AST
 * to ErasedTranslationUnit using .erase()
 */
typealias ClangTranslationUnit = TranslationUnit<CXCursor, CXCursor>

class ClangUnitState(
    val tuid: TUID,
    val cxunit: CXTranslationUnit
) : UnitState<CXCursor, CXCursor> {

    private val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
    private val parser = CursorParser(tuid)

    override fun close() = cxunit.close()

    override val ast by lazy {
        with (parser) {
            rootCursor
                .asTranslationUnit()
                .mapLeft { Throwable(it) }
        }
    }

    override val dependencies: DependencyAnalysis<CXCursor, CXCursor> by lazy {
        // force parsing to populate the declaration and resolution table of the parser
        ast

        ClangDependencyAnalysis(
            tuid,
            parser.declarations.toMutableMap(),
            parser.resolutions.toMutableMap()
        )
    }

    val printer: AstWriters<CXCursor, CXCursor> = AstWriters(
        { c -> clang.clang_getCursorPrettyPrinted(c, null).string.right() },
        { c -> clang.clang_getCursorPrettyPrinted(c, null).string.right() }
    )
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
