@file:Suppress("NAME_SHADOWING")

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
fun typeDependencies(type: Type, needSize: Boolean = true): TypeContext =
    typeDependencies(type, needSize, MutTypeContext())

fun typeDependencies(
    type: Type,
    needSize: Boolean = true,
    acc: MutTypeContext
): MutTypeContext {
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
            is Type.Struct     -> acc.add(next, nextSized)
            is Type.Union      -> acc.add(next, nextSized)
            is Type.Enum       -> acc.add(next, nextSized)
            is Type.Typedeffed -> acc.add(next, nextSized)
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

    return acc
}

/**
 * Extract the set of *direct* type dependencies of [exp].
 * That is, the type context that [exp] demands for it to be itself a valid expression.
 */
fun typeDependencies(stmt: Stmt, acc: MutTypeContext): MutTypeContext = when (stmt) {
    is Stmt.Block       -> stmt.stmts.fold(acc) { acc, s -> typeDependencies(s, acc) }
    is Stmt.Conditional -> {
        typeDependencies(stmt.thenBranch, acc)
        typeDependencies(stmt.elseBranch, acc)
        typeDependencies(stmt.condition, acc)
    }

    is Stmt.Decl        -> {
        stmt.init.tap { typeDependencies(it, acc) }
        acc
    }

    is Stmt.Do          -> typeDependencies(stmt.todo, acc)
    is Stmt.While       -> typeDependencies(stmt.body, typeDependencies(stmt.condition, acc))
    is Stmt.DoWhile     -> typeDependencies(stmt.body, typeDependencies(stmt.condition, acc))
    is Stmt.For         -> {
        typeDependencies(stmt.condition, acc)
        typeDependencies(stmt.before, acc)
        typeDependencies(stmt.after, acc)
        typeDependencies(stmt.body, acc)
    }

    is Stmt.Labeled     -> typeDependencies(stmt.stmt, acc)
    is Stmt.Return      -> {
        stmt.value.tap { typeDependencies(it, acc) }
        acc
    }

    is Stmt.Seq         -> {
        typeDependencies(stmt.first, acc)
        typeDependencies(stmt.second, acc)
    }

    is Stmt.Switch      -> {
        typeDependencies(stmt.scrutinee, acc)
        typeDependencies(stmt.body, acc)
    }

    else                -> acc
}

/**
 * Extract the set of *direct* type dependencies of [exp].
 * That is, the type context that [exp] demands for it to be itself a valid expression.
 */
fun typeDependencies(exp: Exp, ): TypeContext = typeDependencies(exp, MutTypeContext())
fun typeDependencies(exp: Exp, acc: MutTypeContext): MutTypeContext = when (exp) {
    // as per the C spec(type), alignof(type) and sizeof expressions
    // require that the size of the type is computable.
    is Exp.Alignof     -> typeDependencies(exp.type, true, acc)
    is Exp.Sizeof      -> typeDependencies(exp.type, true, acc)

    is Exp.BinOp       -> typeDependencies(exp.right, typeDependencies(exp.left, acc))
    is Exp.Call        -> exp.args
        .fold(acc) { acc, arg -> typeDependencies(arg, acc) }
    is Exp.Cast        -> typeDependencies(exp.exp, typeDependencies(exp.toType, false, acc))
    is Exp.Compound    -> typeDependencies(exp.initializer, acc)
    is Exp.Conditional -> {
        typeDependencies(exp.thenBranch, acc)
        typeDependencies(exp.elseBranch, acc)
        typeDependencies(exp.condition, acc)
    }
    is Exp.UnOp        -> typeDependencies(exp.operand, acc)

    // leafs
    is Exp.Const       -> acc
    is Exp.Var         -> acc
}

private fun typeDependencies(
    initializer: Initializer,
    acc: MutTypeContext
): MutTypeContext {
    when (initializer) {
        is Initializer.InitArray  ->
            initializer.exps.forEach {
                typeDependencies(it, acc)
            }
        is Initializer.InitStruct ->
            initializer.initializers.values.forEach { typeDependencies(it, acc) }
        is Initializer.InitUnion  ->
            typeDependencies(initializer.initializer, acc)
        is Initializer.InitSingle ->
            typeDependencies(initializer.exp)
    }

    return acc
}

fun typeDependencies(decl: UnitDeclaration<Exp, Stmt>): TypeContext =
    typeDependencies(decl, MutTypeContext())
fun typeDependencies(
    decl: UnitDeclaration<Exp, Stmt>,
    acc: MutTypeContext
): MutTypeContext = when (decl) {
    is UnitDeclaration.Compound -> {
        decl.fields.tap {
            it.forEach { field ->
                typeDependencies(field, acc)
            }
        }
        acc
    }

    is UnitDeclaration.Typedef ->
        typeDependencies(decl.underlyingType, false, acc)

    is UnitDeclaration.Fun -> {
        typeDependencies(decl.type, false, acc)
        decl.body.tap { typeDependencies(it, acc) }
        acc
    }

    else -> acc
}

private fun typeDependencies(field: Field, acc: MutTypeContext): MutTypeContext {
    when (field) {
        is Field.Anonymous -> {
            field.subfields.forEach { typeDependencies(it, acc) }
        }
        is Field.Named     ->
            // we need the size of the field type to be able to compute the size of the
            // surrounding struct
            typeDependencies(field.type, true, acc)
    }

    return acc
}
