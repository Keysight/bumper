package com.riscure.bumper.libclang

import arrow.core.*
import com.riscure.bumper.analyses.UnitDependencyAnalysis
import com.riscure.bumper.analyses.Result
import com.riscure.bumper.analyses.nil
import com.riscure.bumper.analyses.union
import com.riscure.bumper.ast.*
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXTranslationUnit
import org.bytedeco.llvm.global.clang.*
import java.nio.file.Path

/**
 * An implementation of the dependency analysis interface using Libclang.
 */
class ClangDependencyAnalysis(
    private val ast: ClangTranslationUnit,

    val cxTranslationUnit: CXTranslationUnit,

    /** A mapping from clang cursors (identified by their hash) and declarations in the [ast] */
    private val elaboratedCursors: Map<CursorHash, ClangDeclaration>,

    /**
     * workingDir is the working directory for clang during source file parsing. CXCursor may have relative paths,
     * like file.string from clang_getPresumedLocation, with workingDir as base.
     */
    private val workingDir: Path
) : UnitDependencyAnalysis<CXCursor, CXCursor> {

    private val tuid get() = ast.tuid

    private fun typeOf(cursor: CXCursor): Either<String, Type> =
        with (CursorParser(tuid, cxTranslationUnit, workingDir)) {
            cursor.type().asType()
        }

    private fun CXCursor.refDependencies(): Result<TLID> {
        // let libclang resolve the reference
        val def = clang_getCursorReferenced(this)
        // Check whether we elaborated a declaration from the defining cursor.
        return when (val decl = elaboratedCursors.getOrNone(def.hash())) {
            is Some<ClangDeclaration> -> {
                // We elaborated a declaration from the defining cursor.
                setOf(decl.value.tlid).right()
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
                        .map { setOf(it.enum) }
                        // if resolve did not return an Enumerator,
                        // it may have been a local declaration, and no global dependencies are induced.
                        .getOrElse { setOf() }
                        .right()
                } else {
                    nil()
                }
            }
        }
    }

    private fun cursorDependencies(cursor: CXCursor): Result<TLID> =
        cursor.fold(nil(), true) { acc: Result<TLID> ->
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
            else -> nil()
        })
    }

    override fun ofExp(exp: CXCursor)   = cursorDependencies(exp)
    override fun ofStmt(stmt: CXCursor) = cursorDependencies(stmt)
}