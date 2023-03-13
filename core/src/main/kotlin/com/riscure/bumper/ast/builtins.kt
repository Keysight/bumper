package com.riscure.bumper.ast

/**
 * Platform dependent compiler builtins
 */
interface Builtins {
    val __builtin_va_list: UnitDeclaration.Typedef

    val builtinTypedefs: List<UnitDeclaration.Typedef>
        get() = listOf(__builtin_va_list)
}

// Inspired by LGPL Compcert definitions
// https://github.com/AbsInt/CompCert/blob/04f499c632a76e460560fc9ec4e14d8216e7fc18/aarch64/CBuiltins.ml
object Aarch64: Builtins {
    override val __builtin_va_list
        // TODO this is only true apparently for AAPCS64 Abi and not on MacOSX?
        get() = UnitDeclaration.Typedef("__builtin_va_list", Type.void.ptr())
}