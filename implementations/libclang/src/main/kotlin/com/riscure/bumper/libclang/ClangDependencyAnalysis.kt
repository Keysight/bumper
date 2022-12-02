package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.UnitDependencyAnalysis
import com.riscure.bumper.analyses.Result
import com.riscure.bumper.analyses.union
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

/**
 * An implementation of the dependency analysis interface using Libclang.
 */
class ClangDependencyAnalysis(
    private val tuid: TUID,
    private val elaboratedCursors: Map<CursorHash, ClangDeclaration>
) : UnitDependencyAnalysis<CXCursor, CXCursor> {

    private fun typeOf(cursor: CXCursor): Either<String, Type> =
        with (CursorParser(tuid)) {
            cursor.type().asType()
        }

    private fun CXCursor.refDependencies(): Result {
        // let libclang resolve the reference
        val def = clang_getCursorReferenced(this)

        // Check whether we elaborated a declaration from the defining cursor.
        // If not, we are dealing with a (reference to) a local declaration,
        // and we don't include those.
        return when (val decl = elaboratedCursors.getOrNone(def.hash())) {
            is Some -> setOf(decl.value.mkSymbol(tuid)).right()
            // otherwise we assume it is a local declaration
            // which results in no additional dependencies.
            is None -> nil
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result =
        cursor.fold(nil, true) { acc: Result ->
            acc.union(
                when (kind()) {
                    CXCursor_VarDecl     ->
                        typeOf(this)
                            .flatMap { ofType(it) }
                    CXCursor_DeclRefExpr ->
                        refDependencies()
                    CXCursor_MemberRef   ->
                        TODO()
                    else                 -> nil
                }
            )
        }

    override fun ofExp(exp: CXCursor): Result = cursorDependencies(exp)
    override fun ofStmt(stmt: CXCursor): Result = cursorDependencies(stmt)
}