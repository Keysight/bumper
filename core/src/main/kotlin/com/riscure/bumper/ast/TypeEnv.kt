package com.riscure.bumper.ast

import arrow.core.*
import com.riscure.bumper.sources.Preamble
import com.riscure.dobby.clang.Include
import com.riscure.dobby.clang.IncludePath

/** The type of results for type definition lookups */
typealias TypeLookup<T> = Either<TypeEnv.Missing, T>

/** A mapping of types to the best available definitions */
typealias TypeResolutions = Map<Type.Defined, UnitDeclaration.TypeDeclaration>

/** A type context is an interface that declares some types to be in scope */
fun interface TypeContext {
    /**
     * A mapping from types in this context to a boolean indicating whether they
     * are fully defined (value = true), or just declared (value = false).
     */
    fun get(): Map<Type.Defined, Boolean>
}

/**
 * A type environment is an interface that gives access to type declarations and/or definitions.
 */
interface TypeEnv {
    data class Missing(
        val type: TLID,

        /**
         * [needDefinition] is true if we know that a definition for [type] was required, rather
         * than just a declaration.
         */
        val needDefinition: Boolean
    ): Exception() {
        override val message: String
            get() {
                val needs = if (needDefinition) "definition" else "declaration"
                return "Failed to lookup $needs of ${type.pretty}"
            }
    }

    val builtins: Builtins

    /**
     * Return the most precise type declaration of the type identified by [tlid].
     * That is, if this [TypeEnv] has a definition, that definition is returned.
     * Otherwise, it may return a declaration. If neither is available, we return [Missing].
     */
    fun lookup(tlid: TLID): TypeLookup<UnitDeclaration.TypeDeclaration>

    /**
     * Return the most precise type declaration of [type].
     * That is, if this [TypeEnv] has a definition, that definition is returned.
     * Otherwise, it may return a declaration. If neither is available, we return [Missing].
     */
    fun lookup(type: Type.Defined): TypeLookup<UnitDeclaration.TypeDeclaration> =
        lookup(type.ref)
            .filterOrOther({ it.kind == type.kind }) {
                Missing(type.ref, false) // defined types need not have a definition
            }

    /**
     * Resolve a given [ctx] in this type environment, producing a set of
     * type declarations/definitions whose keys coincide with the keys of `[ctx].get()`.
     * If the context requires a definition, then the returned map if any is guaranteed to
     * have a [UnitDeclaration.TypeDeclaration] such that [isDefined] is true.
     */
    fun resolve(ctx: TypeContext): TypeLookup<TypeResolutions> =
        try {
            ctx
                .get()
                .mapValues { (typ, needsDefinition) ->
                    lookup(typ)
                        .filterOrOther({ !needsDefinition || it.isDefinition }) {
                            Missing(typ.ref, needsDefinition)
                        }
                        .getOrHandle { throw it }
                }
                .right()
        }
        catch (e : Missing) { e.left() }

    fun typedefs(tlid: TLID): TypeLookup<UnitDeclaration.Typedef> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Typedef) it.right()
                else Missing(tlid, true).left()
            }
    fun structs(tlid: TLID): TypeLookup<UnitDeclaration.Struct> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Struct) it.right()
                else Missing(tlid, false).left()
            }
    fun unions(tlid: TLID): TypeLookup<UnitDeclaration.Union> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Union) it.right()
                else Missing(tlid, false).left()
            }
    fun enums(tlid: TLID): TypeLookup<UnitDeclaration.Enum> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Enum) it.right()
                else Missing(tlid, false).left()
            }
    fun fields(tlid: TLID): TypeLookup<FieldDecls> =
        lookup(tlid)
            .flatMap {
                if (it is UnitDeclaration.Compound) it.fields.toEither {
                    Missing(tlid, true)
                }
                else Missing(tlid, true).left()
            }
}

/**
 * Compute the best preamble for [this] collection of type resolutions.
 *
 * If the type resolved to a declaration in a header, include it.
 * If the type resolved to a declaration (not definition!) in a C file, declare it.
 * If the type resolved to a definition in a C file, include the C file.
 *
 * Fails if the type resolutions contain definitions that have no location data.
 *
 * @return [Pair] of produced preamble, and a Set of types for which we had to include a C file.
 */
fun TypeResolutions.toPreamble(
    includePath: IncludePath,

    /**
     * Optionally you can pass a TypeContext that will be used to check if a size *needs* to
     * be known for the type resolutions.
     */
    required: TypeContext = TypeContext { mapOf() }
): Either<Exception, Pair<Preamble, Set<Type.Defined>>> = try {
    val includes  = mutableSetOf<Include>()
    val typeDecls = mutableSetOf<UnitDeclaration.TypeDeclaration>()
    val warnings  = mutableSetOf<Type.Defined>()

    // compute the optimal preamble entry for every type resolution
    this.forEach { (type, it) ->
        val include = it.meta
            .presumedLocation
            // try to resolve it on the include path
            .flatMap { includePath.relativize(it.sourceFile) }
            .getOrElse {
                // I think this should not happen, because [ast] is supposed to come
                // from the parser.
                throw Exception(
                    "Failed to get source location on include path for type " +
                        "dependency ${type.ref.pretty}"
                )
            }

        val needSize = required.get().getOrDefault(type, it.isDefinition)
        when {
            // if we have a definition from a header, we are golden.
            include.isHeader -> includes.add(include)
            // if we don't need the size, we can just declare it locally, unless it's a typedef.
            !needSize -> when (type) {
                is Type.Struct     -> typeDecls.add(UnitDeclaration.Struct(type.ref.name))
                is Type.Union      -> typeDecls.add(UnitDeclaration.Union(type.ref.name))
                is Type.Enum       -> typeDecls.add(UnitDeclaration.Enum(type.ref.name))
                is Type.Typedeffed -> {
                    includes.add(include)
                    warnings.add(type)
                }
            }
            // worst case we include the C file.
            else -> {
                includes.add(include)
                warnings.add(type)
            }
        }
    }

    Pair(Preamble(includes.toList(), typeDecls), warnings).right()
} catch (err: Exception) { err.left() }