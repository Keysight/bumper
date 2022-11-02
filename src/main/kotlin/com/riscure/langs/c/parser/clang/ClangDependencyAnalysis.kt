package com.riscure.langs.c.parser.clang

import arrow.core.*
import com.riscure.langs.c.analyses.Result
import com.riscure.langs.c.ast.TLID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

class ClangDependencyAnalysis: com.riscure.langs.c.analyses.DependencyAnalysis<CXCursor, CXCursor>() {

    fun CXCursor.refDependencies(): Result {
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
                        .flatMap { it.getDependencies() }
                        .merge(acc)
                CXCursor_DeclRefExpr ->
                    refDependencies()
                        .merge(acc)
                CXCursor_MemberRef   -> acc // TODO
                else                 -> acc
            }
        }

    override fun CXCursor.expDependencies (): Result = cursorDependencies(this)
    override fun CXCursor.stmtDependencies(): Result = cursorDependencies(this)
}