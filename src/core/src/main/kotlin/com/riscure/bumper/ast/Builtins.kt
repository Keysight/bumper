package com.riscure.bumper.ast

/**
 * Platform dependent compiler builtins.
 * These are **standardized** builtin definitions.
 *
 * For some analyses we depend on knowing what builtins the compiler assumes.
 * In that case we can depend on a particular implementation of this interface.
 */
interface Builtins {
    val __builtin_va_list: UnitDeclaration.Typedef

    val builtinTypedefs: List<UnitDeclaration.Typedef>
        get() = listOf(__builtin_va_list)


    // Be inspired by LGPL Compcert definitions
    // https://github.com/AbsInt/CompCert/blob/04f499c632a76e460560fc9ec4e14d8216e7fc18/aarch64/CBuiltins.ml
    companion object {
        @JvmField
        val clang = object:Builtins {
            override val __builtin_va_list =
                UnitDeclaration.Typedef("__builtin_va_list", Type.array(Type.struct("__va_list_tag"), 1L))
        }
    }
}
