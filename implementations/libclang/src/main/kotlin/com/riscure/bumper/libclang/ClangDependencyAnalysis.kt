package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.DependencyAnalysis
import com.riscure.bumper.analyses.Result
import com.riscure.bumper.analyses.union
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

/**
 * An implementation of the dependency analysis interface using Libclang.
 */
/*class ClangDependencyAnalysis(
    tuid: TUID,

    // The following two tables should be prepopulated by all contextual
    // declarations/definitions for the variable/function bodies that we
    // may analyze.
    // These tables are computed by the [CursorParser] as it produces the AST.
    declarationTable: MutableMap<CursorHash, ClangDeclaration>,
    resolutionTable: MutableMap<Declaration<*, *>, CursorHash>
) : CursorParser(tuid, declarationTable, resolutionTable),
    DependencyAnalysis<CXCursor, CXCursor> {

    /**
     * Reference result in dependencies on other symbols.
     * The reference is resolved semantically by libclang.
     */
    private fun CXCursor.refDependencies(): Result {
        val def = clang_getCursorDefinition(this)

        return if (def.isLocalDefinition()) {
            nil
        } else {
            // FIXME
            // if the cursor references a local declaration,
            // the table may not have a declaration yet.
            when (val sym = declarationTable[clang_hashCursor(def)].toOption().flatMap { it.mkSymbol(tuid) }) {
                is Some -> setOf(sym.value).right()
                is None -> "Could not resolve reference `${spelling()}`".left()
            }
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result =
        cursor.fold(nil, true) { acc: Result ->
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
}*/