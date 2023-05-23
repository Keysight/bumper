package com.riscure.bumper.ast

import com.riscure.bumper.ast.Exp.Companion.call
import com.riscure.bumper.ast.Exp.Companion.sizeOf
import com.riscure.bumper.ast.Type.Companion.function
import com.riscure.bumper.ast.Type.Companion.void
import com.riscure.bumper.sources.Include
import com.riscure.bumper.sources.Preamble
import org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS


interface IStdlib {
    /** A preamble that needs to be included in the source for this stdlib model to be meaningful */
    val preamble: Preamble

    val size_t: Type

    /**
     * A precondition for [sizeof] is that [type] is complete so that the compiler can determine its size.
     * It is up to the caller to verify that this precondition holds at the point where sizeof is called.
     */
    fun sizeof(type: Type) = sizeOf(type, size_t)

    val malloc get() = UnitDeclaration.Fun<Nothing>(
        "malloc", function(void.ptr(), Param("size", size_t))
    )

    fun malloc(sizeExp: Exp): Exp = call(malloc.ref(), sizeExp)

    /**
     * A precondition for [malloc] is that [type] is complete so that the compiler can determine its size.
     * It is up to the caller to verify that this precondition holds at the point where sizeof is called.
     */
    fun malloc(type: Type)= malloc(sizeof(type))
}

/**
 * This is a target-specific implementation of the [IStdlib] interface,
 * modeling every part of it completely. In other words, it does not depend on any headers.
 */
class Stdlibs(val sizeKind: IKind): IStdlib {
    override val size_t: Type = Type.Int(sizeKind)

    @JvmField val free = UnitDeclaration.Fun<Nothing>("free", function(void.ptr()))

    override fun malloc(sizeExp: Exp) = call(malloc.ref(), sizeExp)

    override val preamble = Preamble(
        listOf(),
        listOf(
            malloc,
            free,
        )
    )

    companion object {
        // these choices are tested by BumperStdlibTest
        @JvmStatic val x64Linux = Stdlibs(IKind.IULong)
        @JvmStatic val x64Win   = Stdlibs(IKind.IULongLong)

        /** The standard library implementation for any (supported) 64-bit host platform */
        @JvmStatic val x64host: Stdlibs = if (IS_OS_WINDOWS) x64Win else x64Linux
    }
}

/**
 * An implementation of [Stdlibs] that is agnostic of the platform (not choosing any
 * particular implementation of opaque types), but dependent on inclusion of the [headers]
 */
object StdlibsHeader: IStdlib {
    override val preamble = Preamble(
        listOf(
            Include("<stddef.h>"),
            Include("<stdlib.h")
        ),
        listOf()
    )

    override val size_t: Type = Type.typedef("size_t")
    val NULL: Exp  = Exp.Var("NULL", void.ptr())
}
