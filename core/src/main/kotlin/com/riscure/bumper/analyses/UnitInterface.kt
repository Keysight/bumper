package com.riscure.bumper.analyses

import com.riscure.bumper.ast.Storage
import com.riscure.bumper.ast.TranslationUnit
import com.riscure.bumper.ast.UnitDeclaration

/**
 * A [UnitInterface] is set of imports (declarations without definitions in the unit)
 * together with a set of exports (defined and visible/non-static symbols in the unit).
 */
data class UnitInterface<E, S>(
    val imports: Set<UnitDeclaration.Valuelike<E, S>>,
    val exports: Set<UnitDeclaration.Valuelike<E, S>>,
)

/**
 * Compute the [UnitInterface] of a translation [unit].
 */
fun <E, S> objectInterface(unit: TranslationUnit<E, S>): UnitInterface<E, S> {
    // compute the list of declarations that are visible
    // across linked units
    val exports = unit
        .valuelikeDefinitions
        .filter { decl -> decl.storage != Storage.Static }
    val exportIndex = exports.map { it.tlid } .toSet()

    val imports = unit
        .valuelikeDeclarations
        .filter { decl -> decl.storage != Storage.Static }
        .filter { decl -> !exportIndex.contains(decl.tlid) }

    return UnitInterface(imports.toSet(), exports.toSet())
}
