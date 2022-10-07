package com.riscure.langs.c.parser.clang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.langs.c.ast.Location
import com.riscure.langs.c.ast.SourceRange
import com.riscure.langs.c.ast.TopLevel
import com.riscure.langs.c.parser.*
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.nio.file.Path

typealias Result<T> = Either<Throwable, T>

class ClangUnitState(val cxunit: CXTranslationUnit) : UnitState {

    private val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
    private val _ast by lazy { rootCursor.asTranslationUnit() }

    // We have to keep track of this, because clang_getCursor(CXLocation)
    // maps to the innermost ast element at that location.
    // Although we can step up from that to the lexical or semantic parent,
    // we cannot reliably find the enclosing top-level declaration using the libclang API.
    private val tlLocationToCursor: Map<Location, CXCursor> =
        rootCursor
            .topLevelCursors()
            .flatMap { tlc ->
                tlc.getStart()
                    .map { Pair(it, tlc) }
                    .toList()
            }
            .toMap()

    override fun close() = cxunit.close()

    override fun ast() = _ast.mapLeft { Throwable(it) }

    /**
     * Get the cursor representing a toplevel declaration using
     * [tl].meta.location.
     */
    fun getCursor(tl: TopLevel): Option<CXCursor> = tl.meta.location
        .map { it.begin }
        .flatMap { tlLocationToCursor[it].toOption() }

    /**
     * Map a cursor to the top-level entity that encloses it in the source, without reparsing.
     */
    private fun getTopLevel(cursor: CXCursor): Result<TopLevel> =
        cursor.right()
            .flatMap { it.getRange().toEither { Throwable("Failed to get location for cursor") }}
            .flatMap { range ->
                ast().flatMap { ast ->
                    ast.getAtLocation(range.begin).toEither { Throwable("Failed to get AST node at location") }
                }
            }

    override fun getReferencedToplevels(decl: TopLevel): Result<Set<TopLevel>> =
        decl
            .getCursor().toEither { Throwable("Failed to get cursor for toplevel declaration") }
            .flatMap { cursor ->
                cursor
                    // collect cursors for nodes that are references
                    .collect(Monoid.list(), true) { getReferenced().toList() }
                    // only keep those that reference top-level entities
                    .filter { it.isTopLevelEntity() }
                    .map { getTopLevel(it) }
                    .sequenceEither() // bubble errors up
                    .map { it.toSet() }
            }

    override fun getSource(decl: TopLevel): Option<String> =
        decl.getCursor()
            .filter { !it.isNull }
            .map { cursor -> clang.clang_getCursorPrettyPrinted(cursor, null) }
            .filter { !it.isNull }
            .map { it.string }
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

/**
 * Returns the cursor that represent this top-level,
 */
context(ClangUnitState)
fun TopLevel.getCursor(): Option<CXCursor> = getCursor(this)

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
