package com.riscure.langs.c.parser.clang

import arrow.core.Option
import com.riscure.langs.c.ast.Location
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.asTranslationUnit
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.io.File
import java.nio.file.Path

class ClangUnitState(val cxunit: CXTranslationUnit) : UnitState {

    val cursor = clang.clang_getTranslationUnitCursor(cxunit)
    val _ast by lazy { cursor.asTranslationUnit() }

    override fun close() {
        cxunit.close()
    }

    override fun ast() = _ast

    override fun getSource(decl: TopLevel): Option<String> =
        decl.meta.location.map {
            val cursor = it.begin.getCursor()
            clang.clang_getCursorPrettyPrinted(cursor, clang.clang_getCursorPrintingPolicy(cursor)).string
        }

}

/**
 * In the context of a ClangUnitState we can convert some things
 * back to their libclang counterparts.
 */

context(ClangUnitState)
fun Location.getCursor()  =
    clang.clang_getCursor(cxunit, cx())

context(ClangUnitState)
fun TopLevel.getCursor(): Option<CXCursor>  =
    meta.location
        .map { it.begin.getCursor() }

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
