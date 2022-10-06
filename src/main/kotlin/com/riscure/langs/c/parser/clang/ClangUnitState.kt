package com.riscure.langs.c.parser.clang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.langs.c.ast.Location
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.parser.UnitState
import com.riscure.langs.c.parser.asTranslationUnit
import com.riscure.langs.c.parser.getStart
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.nio.file.Path

typealias Result<T> = Either<Throwable, T>

class ClangUnitState(val cxunit: CXTranslationUnit) : UnitState {

    val cursor = clang.clang_getTranslationUnitCursor(cxunit)
    val _ast by lazy { cursor.asTranslationUnit() }

    override fun close() {
        cxunit.close()
    }

    override fun ast() = _ast.mapLeft { Throwable(it) }

    private fun getToplevelFromCursor(cursor: CXCursor): Result<TopLevel> =
        cursor.right()
            .filterOrElse({ it.isTopLevelEntity() }, { Throwable("Not a top-level entity" )})
            .flatMap {cursor ->
                ast().flatMap { ast ->
                    cursor
                        .getStart().toEither { Throwable("No location for cursor.") }
                        .flatMap { ast.getByLocation(it).toEither { Throwable("No top-level declaration in AST with that location.") } }
                }
            }

    override fun getReferencedToplevels(decl: TopLevel): Result<Set<TopLevel>> =
        decl
            .getCursor().toEither { Throwable("Failed to get cursor for toplevel declaration") }
            .flatMap { cursor ->
                cursor
                    // collect cursors for nodes that are references
                    .collect(Monoid.list(), true) {
                        if (isReference()) listOf(getReferenced()) else { listOf()}
                    }
                    // only keep those that reference top-level entities
                    .filter { it.isTopLevelEntity() }
                    .map { getToplevelFromCursor(it) }
                    .sequenceEither() // bubble errors up
                    .map { it.toSet() }
            }

    override fun getSource(decl: TopLevel): Option<String> =
        decl.meta.location
            .map { it.begin.getCursor() }
            .filter { !it.isNull }
            .map { cursor -> clang.clang_getCursorPrettyPrinted(cursor, null) }
            .filter { !it.isNull }
            .map { it.string }
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
