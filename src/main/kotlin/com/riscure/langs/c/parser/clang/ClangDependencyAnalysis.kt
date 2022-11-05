package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.analyses.DependencyAnalysis
import com.riscure.langs.c.analyses.Result
import com.riscure.langs.c.analyses.union
import com.riscure.langs.c.ast.ErasedDeclaration
import com.riscure.langs.c.ast.Site
import com.riscure.langs.c.index.Symbol
import com.riscure.langs.c.index.TUID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

class ClangDependencyAnalysis(
    tuid: TUID,
    declarationTable: MutableMap<CursorHash, ClangDeclaration>,
    resolutionTable: MutableMap<ErasedDeclaration, CursorHash>
) : CursorParser(tuid, declarationTable, resolutionTable),
    DependencyAnalysis<CXCursor, CXCursor> {


    private fun CXCursor.refDependencies(): Result {
        val def = clang_getCursorDefinition(this)

        return if (def.isLocalDefinition()) {
            nil
        } else {
            when (val sym = declarationTable[clang_hashCursor(def)].toOption().flatMap { it.mkSymbol(tuid) }) {
                is Some -> setOf(sym.value).right()
                is None -> "Could not resolve reference `${spelling()}`".left()
            }
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result =
        cursor.fold(nil, true) { acc:Result ->
            when (kind()) {
                CXCursor_VarDecl     ->
                    this.type()
                        .asType(Site.local)
                        .flatMap { ofType(it) }
                        .union(acc)
                CXCursor_DeclRefExpr ->
                    refDependencies()
                        .union(acc)
                CXCursor_MemberRef   ->
                    TODO()
                else                 -> acc
            }
        }

    override fun ofExp(exp: CXCursor): Result = cursorDependencies(exp)
    override fun ofStmt(stmt: CXCursor): Result = cursorDependencies(stmt)
}