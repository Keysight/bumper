/**
 * Some extension methods for working with bytedeco's
 * libclang cursor pointer thing.
 */
package com.riscure.langs.c.parser.clang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.toBool
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*

fun CXCursor.spelling(): String = clang_getCursorSpelling(this).string
fun CXCursor.kindName(): String = clang_getCursorKindSpelling(kind()).string
fun CXType.kindName(): String = clang_getTypeKindSpelling(kind()).string

/**
 * Get the list of child cursors from a cursor.
 */
fun CXCursor.children(): List<CXCursor> = fold(monoid = Monoid.list(), false) { listOf(this) }

fun CXCursor.type(): CXType = clang_getCursorType(this)

fun <T> CXCursor.fold(acc: T, recursive: Boolean, visitor: CXCursor.(acc: T) -> T): T {
    var accumulator = acc

    val wrapped = object: CXCursorVisitor() {
        override fun call(self: CXCursor?, parent: CXCursor?, p2: CXClientData?): Int {
            accumulator = visitor(self!!, accumulator)
            return if (recursive) CXChildVisit_Recurse else CXChildVisit_Continue
        }
    }

    try { clang_visitChildren(this, wrapped, null) }
    finally { wrapped.deallocate() }

    return accumulator
}

/**
 * Collect some data from the [recursive] children of a cursor.
 * The data we collect should implement the monoid typeclass,
 * so that we can combine them when we fold up the tree.
 */
fun <T> CXCursor.fold(monoid: Monoid<T>, recursive: Boolean, visitor: CXCursor.() -> T): T =
    fold(monoid.empty(), recursive) { acc ->
        val cursor = this
        with(monoid) {
            acc.combine(visitor(cursor))
        }
    }

fun CXCursor.getExtent(): Option<CXSourceRange> {
    val ptr = clang_getCursorExtent(this)
    return if (ptr.isNull) None else ptr.some()
}

fun CXCursor.filterNullCursor(): Option<CXCursor>  =
    if (clang_Cursor_isNull(this).toBool() || this.isNull) {
        None
    } else this.some()

fun CXCursor.isNullCursor() = filterNullCursor().isEmpty()

/**
 * Get the cursor of the definition that is being referenced by [this] cursor.
 *
 * WARNING: Be careful how you use this. Clang happily returns cursors for many expressions that don't appear like
 * references. Which cursor is returned exactly is also not always predictable.
 */
fun CXCursor.getReferenced(): Option<CXCursor> = clang_getCursorReferenced(this).filterNullCursor()

@Deprecated("Useless: clang returns false for cursors of kind DeclRefExpr!?")
fun CXCursor.isReference() =
    // This is not the same as getReferenced().isDefined
    // because clang returns something from getCursorReferenced for various cursors
    // that are not references
    clang_isReference(kind()).toBool()

fun CXCursor.semanticParent() = clang_getCursorSemanticParent(this)
fun CXCursor.lexicalParent() = clang_getCursorLexicalParent(this)
fun CXCursor.translationUnit() = clang_getTranslationUnitCursor(clang_Cursor_getTranslationUnit(this))

fun CXType.spelling(): String = clang_getTypeSpelling(this).string

// TODO needs testing
fun CXCursor.isLocalDefinition(): Boolean {
    // Checking the inverse (that the semanticParent is the translation unit)
    // does not work, because libclang sometimes returns some InvalidFile (?) cursor for some declarations.
    val parent = semanticParent().kind()
    return (clang_isStatement(parent).toBool() || parent == CXCursor_FunctionDecl)
}

fun CXCursor.isGlobalDefinition(): Boolean = !isLocalDefinition()