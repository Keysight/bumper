package com.riscure.bumper.parser

import arrow.core.Either
import arrow.core.right
import com.riscure.bumper.analyses.DependencyGraph
import com.riscure.bumper.ast.*
import com.riscure.bumper.index.TUID
import com.riscure.bumper.preprocessor.CPPInfo
import java.io.Closeable

/**
 * The interface to the state of the parser. This negotiates between
 * the True Code representation of C programs (i.e., [TranslationUnit] and its siblings),
 * and the third-party parser representation of the same programs.
 *
 * It is parameterized by the types of expressions and statements that make up definitions.
 *
 * This interface is all that stands between us and the wild-west of libclang, for example.
 *
 * Because parsers may be native libraries, this implements Closeable
 * and you have to promise to properly call close() when you're done with
 * the instance of UnitState.
 */
interface UnitState<Exp,Stmt,Self:UnitState<Exp,Stmt,Self>>: Closeable {
    val cppinfo: CPPInfo
    fun withCppinfo(cppinfo: CPPInfo): Self

    val tuid: TUID get() = ast.tuid
    fun withTUID(tuid: TUID): Self

    /**
     * Convert this translation unit to an AST.
     */
    val ast: TranslationUnit<Exp, Stmt>

    /**
     * The dependency graph for this unit.
     */
    val dependencies: Either<String, DependencyGraph>

    /**
     * An improved [close], that returns a side-effect free
     * projection of the data that can be retrieved from the [UnitState] API.
     */
    fun erase(): Either<String, UnitData>

}

/**
 * The free implementation of the [UnitState] interface
 */
data class UnitData(
    override val ast: TranslationUnit<SourceRange, SourceRange>,
    override val dependencies: Either<String, DependencyGraph>,
    override val cppinfo: CPPInfo = CPPInfo(),
) : UnitState<SourceRange, SourceRange, UnitData> {
    override fun withCppinfo(cppinfo: CPPInfo): UnitData = copy(cppinfo = cppinfo)
    override fun withTUID(tuid: TUID): UnitData = copy(ast = ast.copy(tuid = tuid))
    override fun erase(): Either<String, UnitData> = this.right()
    override fun close() { /* noop */ }
}