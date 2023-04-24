package com.riscure.bumper.ast

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import kotlinx.serialization.Serializable

/** Different ways of storing values in C */
@Serializable
sealed interface Storage {
    sealed interface Public: Storage

    fun isPublic(): Option<Public> = when (this) {
        is Static -> None
        Auto      -> (this as Public).some()
        Default   -> (this as Public).some()
        Extern    -> (this as Public).some()
        Register  -> (this as Public).some()
    }

    @Serializable object Static: Storage
    @Serializable object Default: Public // used for toplevel names without explicit storage
    @Serializable object Extern: Public
    @Serializable object Auto: Public // used for block-scoped names without explicit storage
    @Serializable object Register: Public
}