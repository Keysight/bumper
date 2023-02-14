package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.UnitDependencyAnalysis
import com.riscure.bumper.analyses.Result
import com.riscure.bumper.analyses.union
import com.riscure.bumper.ast.*
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.global.clang.*

/**
 * An implementation of the dependency analysis interface using Libclang.
 */
class ClangDependencyAnalysis(
    private val ast: ClangTranslationUnit,
    private val elaboratedCursors: Map<CursorHash, ClangDeclaration>,
) : UnitDependencyAnalysis<CXCursor, CXCursor> {

    private val tuid get() = ast.tuid

    private fun typeOf(cursor: CXCursor): Either<String, Type> =
        with (CursorParser(tuid)) {
            cursor.type().asType()
        }

    private fun CXCursor.refDependencies(): Result {
        // let libclang resolve the reference
        val def = clang_getCursorReferenced(this)
        // Check whether we elaborated a declaration from the defining cursor.
        return when (val decl = elaboratedCursors.getOrNone(def.hash())) {
            is Some<ClangDeclaration> -> {
                // We elaborated a declaration from the defining cursor.
                setOf(decl.value.mkSymbol(tuid)).right()
            }
            is None -> {
                // This is a CXCursor_DeclRefExpr, maybe it's an enum value?
                if (def.kind() == CXCursor_EnumConstantDecl) {
                    // Get the enumerator in the ast.
                    // Note that this means we have a dependency on the enclosing enum (for all the other currently
                    // written cases we consider the dependency to be what is at the cursor itself).
                    ast.resolve(def.spelling())
                        .filterIsInstance<Enumerator>()
                        // the mention of the enumerator induces a dependency on the surrounding enum
                        .map { setOf(it.enum)}
                        // if resolve did not return an Enumerator,
                        // it may have been a local declaration, and no global dependencies are induced.
                        .getOrElse { setOf() }
                        .right()
                } else {
                    nil
                }
            }
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result =
        cursor.fold(nil, true) { acc: Result ->
            acc.union(
                when (kind()) {
                    CXCursor_TypeRef     ->
                        typeOf(this)
                            .flatMap { ofType(it) }
                    CXCursor_VarDecl     ->
                        typeOf(this)
                            .flatMap { ofType(it) }
                    CXCursor_DeclRefExpr ->
                        refDependencies()
//                    CXCursor_MemberRef   ->
//                        TODO()
            else -> nil
        })
    }

    override fun ofExp(exp: CXCursor): Result = cursorDependencies(exp)
    override fun ofStmt(stmt: CXCursor): Result = cursorDependencies(stmt)
}