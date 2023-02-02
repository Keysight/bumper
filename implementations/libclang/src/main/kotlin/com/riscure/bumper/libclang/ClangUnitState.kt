package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import com.riscure.bumper.parser.UnitData
import com.riscure.bumper.parser.UnitState
import com.riscure.bumper.pp.AstWriters
import com.riscure.bumper.pp.Extractor
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang

/**
 * The Clang parser implementation now only analyzes upto expression/statements.
 * And then remembers the locations for those.
 */
typealias ClangTranslationUnit = TranslationUnit<CXCursor, CXCursor>
typealias ClangDeclaration     = UnitDeclaration<CXCursor, CXCursor>

/**
 * The state of a parsed unit in context of the libclang state of the AST.
 * As soon as you close this UnitState, the cursors are invalidated.
 */
class ClangUnitState(
    override val ast: TranslationUnit<CXCursor, CXCursor>,
    private val cxunit: CXTranslationUnit,
    private val elaboratedCursors: Map<CursorHash, ClangDeclaration>
) : UnitState<CXCursor, CXCursor> {
    override fun close() = cxunit.close()

    // We could use libclang's pretty printing facilities here,
    // except that I've encountered corner cases where pretty printing returns "" incorrectly:
    // - https://github.com/llvm/llvm-project/issues/59155
    // So we fall back here on extracting lines from the source file instead.
    override val printer: AstWriters<CXCursor, CXCursor> = mkPrinter(tuid)
    override fun erase() = Either
        .catch({ e -> close(); e.message!! }) {
            fun rangeExtractor(c: CXCursor) = c.getRange().getOrElse {
                throw Throwable("Failed to extract source ranges of definitions.")
            }

            ast
                .map(::rangeExtractor, ::rangeExtractor)
                .let { ast ->
                    this@ClangUnitState.close()
                    UnitData(ast, dependencies)
                }
        }

    override val dependencies get() =
        ClangDependencyAnalysis(ast, elaboratedCursors).ofUnit(ast)

    companion object {
        @JvmStatic
        fun create(tuid: TUID, cxunit: CXTranslationUnit): Either<String, ClangUnitState> {
            val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
            return with(CursorParser(tuid)) {
                rootCursor
                    .asTranslationUnit()
                    .map { (ast, cursors) -> ClangUnitState(ast, cxunit, cursors) }
            }
        }

        /**
         * Utility function to create the ast pretty printers for a [ClangTranslationUnit].
         */
        @JvmStatic
        fun mkPrinter(tuid: TUID): AstWriters<CXCursor, CXCursor> {
            val extractor = Extractor(tuid.main.toFile())

            fun cursorPrinter(c: CXCursor) =
                c.getRange()
                    .toEither { "Failed to get source range for expression." }
                    .flatMap { extractor.extract(it) }

            return AstWriters(::cursorPrinter, ::cursorPrinter)
        }
    }
}
