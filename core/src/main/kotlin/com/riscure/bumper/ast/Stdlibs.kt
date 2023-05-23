package com.riscure.bumper.ast

import arrow.core.some
import com.riscure.bumper.ast.Exp.Companion.call
import com.riscure.bumper.ast.Exp.Companion.cast
import com.riscure.bumper.ast.Exp.Companion.constant
import com.riscure.bumper.ast.Exp.Companion.sizeOf
import com.riscure.bumper.ast.Type.Companion.function
import com.riscure.bumper.ast.Type.Companion.void
import org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS

/**
 * A collection of references and definitions from the C standard library.
 */
open class Stdlibs(
    val sizeKind: IKind,
    val ptrKind: IKind
) {
    // stdlib typedefs
    @JvmField val size_t = Type.Int(sizeKind)

    // stdlib functions
    @JvmField
    val malloc = UnitDeclaration.Fun<Nothing>("malloc", function(void.ptr(), Param("size", size_t)))
    @JvmField
    val free = UnitDeclaration.Fun<Nothing>("free", function(void.ptr()))

    @JvmField
    val NULL = UnitDeclaration
        .Var("NULL", void.ptr(), cast(void.ptr(), constant(0, ptrKind)).some())
        .withStorage(Storage.Static)

    /**
     * A precondition for [sizeof] is that [type] is complete so that the compiler can determine its size.
     * It is up to the caller to verify that this precondition holds at the point where sizeof is called.
     */
    fun sizeof(type: Type)   = sizeOf(type, sizeKind)

    fun malloc(sizeExp: Exp) = call(malloc.ref(), sizeExp)

    /**
     * A precondition for [malloc] is that [type] is complete so that the compiler can determine its size.
     * It is up to the caller to verify that this precondition holds at the point where sizeof is called.
     */
    fun malloc(type: Type)   = malloc(sizeof(type))

    val header: List<UnitDeclaration<Exp, Stmt>> = listOf(
        malloc,
        free,
        NULL
    )

    companion object {
        // these choices are tested by BumperStdlibTest
        @JvmStatic val x64Linux = Stdlibs(IKind.IULong, IKind.IULongLong)
        @JvmStatic val x64Win   = Stdlibs(IKind.IULongLong, IKind.IULongLong)

        /** The standard library implementation for any (supported) 64-bit host platform */
        @JvmStatic val x64host: Stdlibs = if (IS_OS_WINDOWS) x64Win else x64Linux
    }
}
