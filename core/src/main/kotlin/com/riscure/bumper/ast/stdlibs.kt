package com.riscure.bumper.ast

import com.riscure.bumper.ast.Exp.*
import com.riscure.bumper.ast.Exp.Companion.call
import com.riscure.bumper.ast.Exp.Companion.sizeOf
import com.riscure.bumper.ast.Type.Companion.void
import com.riscure.bumper.ast.Type.Companion.function
import com.riscure.bumper.ast.Type.Companion.typedef

open class Stdlibs(
    val sizeKind: IKind
) {
    val def_size_t = typedef("size_t")
    val def_malloc = Var("malloc", function(void.ptr(), Param("size", def_size_t)))

    fun sizeof(type: Type)   = sizeOf(type, sizeKind)
    fun malloc(sizeExp: Exp) = call(def_malloc, sizeExp)
    fun malloc(type: Type)   = malloc(sizeof(type))

    companion object {
        @JvmStatic
        val x64 = Stdlibs(IKind.IULong)
    }
}
