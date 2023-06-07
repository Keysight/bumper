package com.riscure.bumper.analyses

import com.riscure.bumper.ast.*

private fun gatherDeps(acc: Set<TLID>, type: Type): Set<TLID> =
    acc + when (type) {
        is Type.Struct     -> setOf(type.ref)
        is Type.Union      -> setOf(type.ref)
        is Type.Enum       -> setOf(type.ref)
        is Type.Typedeffed -> setOf(type.ref)
        else -> setOf()
    }

/**
 * Extract the set of *direct* type dependencies of [type].
 * That is, the type context that [type] demands for it to be itself a valid type.
 *
 * @param needSize indicates whether we need the size of [type] to be known.
 */
fun typeDependencies(type: Type, needSize: Boolean = true): TypeContext {
    val dependencies = mutableMapOf<Type.Defined, Boolean>()
    fun dependOn(type: Type.Defined, needSize: Boolean) {
        dependencies[type] = (dependencies.getOrDefault(type, false) || needSize)
    }

    // in the worklist we unfold the recursion of this function.
    // for each type that we need to process, we keep track of the fact of
    // whether we need the size or not.
    val worklist = mutableMapOf(type to needSize)
    fun queue(type: Type, needSize: Boolean) {
        // we don't duplicate work,
        // but we merge the flag that indicates whether we need to be able to tell the size
        // of the work item.
        worklist[type] = (worklist.getOrDefault(type, false) || needSize)
    }

    while (worklist.isNotEmpty()) {
        val next = worklist.keys.first()
        val nextSized = worklist.remove(next)!!

        when (next) {
            is Type.Array      -> queue(next.elementType, nextSized)
            is Type.Struct     -> dependOn(next, nextSized)
            is Type.Union      -> dependOn(next, nextSized)
            is Type.Enum       -> dependOn(next, nextSized)
            is Type.Typedeffed -> dependOn(next, nextSized)
            is Type.Fun        -> {
                queue(next.returnType, nextSized)
                next.params.forEach {
                    queue(it.type, nextSized)
                }
            }
            is Type.Ptr -> {
                // for type names occurring under pointers,
                // we don't need to be able to tell the size.
                queue(next.pointeeType, false)
            }
            else -> {}
        }
    }

    return TypeContext { dependencies }
}
