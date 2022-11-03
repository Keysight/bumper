package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.analyses.DependencyAnalysis
import com.riscure.langs.c.analyses.Result
import com.riscure.langs.c.analyses.union
import com.riscure.langs.c.ast.TLID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

class ClangDependencyAnalysis: DependencyAnalysis<CXCursor, CXCursor> {

    private fun CXCursor.refDependencies(): Result {
        val def = clang_getCursorDefinition(this)

        return if (def.isLocalDefinition()) {
            setOf<TLID>().right()
        } else when (def.kind()) {
            CXCursor_StructDecl -> setOf(TLID.struct(def.spelling())).right()
            CXCursor_VarDecl    -> setOf(TLID.varDecl(def.spelling())).right()
            else                -> setOf<TLID>().right()
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result =
        cursor.fold(setOf<TLID>().right(), true) {acc:Result ->
            when (kind()) {
                CXCursor_VarDecl     ->
                    this.type().asType()
                        .flatMap { ofType(it) }
                        .union(acc)
                CXCursor_DeclRefExpr ->
                    refDependencies()
                        .union(acc)
                CXCursor_MemberRef   -> acc
                else                 -> acc
            }
        }

    override fun ofExp(exp: CXCursor): Result = cursorDependencies(exp)
    override fun ofStmt(stmt: CXCursor): Result = cursorDependencies(stmt)
}