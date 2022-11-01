package com.riscure.langs.c.parser.clang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.langs.c.analyses.Dependencies
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.TUID
import com.riscure.langs.c.parser.*
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
import java.nio.file.Path

typealias Result<T> = Either<Throwable, T>

class ClangUnitState(val tuid: TUID, val cxunit: CXTranslationUnit) : UnitState {

    private val rootCursor = clang.clang_getTranslationUnitCursor(cxunit)
    private val _ast by lazy { rootCursor.asTranslationUnit(tuid).mapLeft { Throwable(it) } }

    override fun close() = cxunit.close()

    override fun ast() = _ast

    override fun getReferencedDeclarations(decl: TLID): Either<Throwable, Set<TLID>> {

        fun extractor(cursor: CXCursor): Either<String, Set<TLID>> {
            val reffed = cursor.collect(Monoid.list(), true) { getReferenced().toList() }
            TODO()
        }

        return _ast
            .flatMap {
                it[decl].toEither { Throwable("Declaration ${decl.name} not found in translation unit ${tuid.main}") }
            }
            .flatMap { d:Declaration<CXCursor,CXCursor> ->
                with (Dependencies(::extractor, ::extractor)) {
                    d
                        .getDependencies()
                        .mapLeft { Throwable(it) }
                }
            }
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
