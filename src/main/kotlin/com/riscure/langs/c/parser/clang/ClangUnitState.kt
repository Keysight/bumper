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

    private val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
    private val _ast by lazy { rootCursor.asTranslationUnit() }

    // We have to keep track of this, because clang_getCursor(CXLocation)
    // maps to the innermost ast element at that location.
    // Although we can step up from that to the lexical or semantic parent,
    // we cannot reliably find the enclosing top-level declaration using the libclang API.
    private val topLevelMap: Map<Location, CXCursor> =
        rootCursor
            .children()
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
    fun getCursor(tl: TopLevel) = tl.meta.location
        .map { it.begin }
        .flatMap { topLevelMap[it].toOption() }

    /**
     * Map a cursor of a top-level entity back to the corresponding
     * [TopLevel] [ast] element (without reparsing it).
     */
    private fun getToplevelEntity(cursor: CXCursor): Result<TopLevel> =
        cursor.right()
            .filterOrElse({ it.isTopLevelEntity() }, { Throwable("Not a top-level entity" )})
            .flatMap { c ->
                ast().flatMap { ast ->
                    c.getStart()
                        .toEither { Throwable("No location for cursor.") }
                        .flatMap {
                            ast.getByLocation(it)
                               .toEither { Throwable("No top-level declaration in AST with that location.") }
                        }
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
                    .map { getToplevelEntity(it) }
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
