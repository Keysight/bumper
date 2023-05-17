package com.riscure.bumper.ast

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right

/** The type of results for type definition lookups */
typealias TypeLookup<T> = Either<TypeEnv.Missing, T>

/**
 * A type environment is an interface that provides a way of accessing type definitions.
 */
interface TypeEnv {
    data class Missing(val type: TLID): Exception() {
        override val message: String
            get() = "Failed to lookup definition of ${type.kind.toString().lowercase()} ${type.name}"
    }

    val builtins: Builtins

    /**
     * Return a definition of the type identified by [tlid].
     */
    fun lookup(tlid: TLID): TypeLookup<UnitDeclaration.TypeDeclaration>

    fun typedefs(tlid: TLID): TypeLookup<UnitDeclaration.Typedef> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Typedef) it.right()
                else Missing(tlid).left()
            }
    fun structs(tlid: TLID): TypeLookup<UnitDeclaration.Struct> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Struct) it.right()
                else Missing(tlid).left()
            }
    fun unions(tlid: TLID): TypeLookup<UnitDeclaration.Union> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Union) it.right()
                else Missing(tlid).left()
            }
    fun enums(tlid: TLID): TypeLookup<UnitDeclaration.Enum> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Enum) it.right()
                else Missing(tlid).left()
            }
    fun fields(tlid: TLID): TypeLookup<FieldDecls> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Compound) it.fields.toEither { Missing(tlid) }
                else Missing(tlid).left()
            }
}