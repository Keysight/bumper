/**
 * This file contains the translation from the libclang API to the
 * JVM-native AST representing a C translation unit.
 */
package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.getOption
import com.riscure.langs.c.ast.*
import org.bytedeco.javacpp.*
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*
import java.nio.file.Path

private typealias Result<T> = Either<String, T>

/**
 * Get the list of child cursors from a cursor.
 */
fun CXCursor.children(): List<CXCursor> {
    val cs = mutableListOf<CXCursor>()
    val visitor = object: CXCursorVisitor() {
        override fun call(self: CXCursor?, parent: CXCursor?, p2: CXClientData?): Int {
            cs.add(self!!)
            return CXChildVisit_Continue
        }
    }

    clang_visitChildren(this, visitor, null)
    visitor.deallocate()

    return cs
}

fun CXString.get(): String = clang_getCString(this).string

fun CXString.getOption(): Option<String> =
    this.some()
        .filter { it.isNull() }
        .map { clang_getCString(this) }
        .filter { it.isNull }
        .map { it.string }

fun CXCursor.spelling(): String = clang_getCursorSpelling(this).string
fun CXCursor.kindName(): String = clang_getCursorKindSpelling(kind()).string

/**
 * Combinator to fail with a consistent message if we have an unexpected cursor kind.
 */
fun <T> CXCursor.ifKind(k: Int, expectation: String, whenMatch: () -> Result<T>): Result<T> {
    if (kind() != k) {
        return "Expected ${expectation}. Got cursor of kind ${kindName()}".left()
    }

    return whenMatch()
}

fun CXCursor.asTranslationUnit(): Result<TranslationUnit> {
    if (this.kind() != CXCursor_TranslationUnit) {
        return "Expected translation unit, got cursor of kind ${this.kindName()}".left()
    }

    return this.children()
        .map { it.asTopLevel() }
        .sequenceEither()
        .map { TranslationUnit(it) }
}

fun CXCursor.asTopLevel(): Result<TopLevel> =
    (when (kind()) {
        CXCursor_FunctionDecl ->
            if (children().any { child -> child.kind() == CXCursor_CompoundStmt })
                asFunctionDef()
            else asFunctionDecl()
        CXCursor_StructDecl   -> this.asStructDecl()
        CXCursor_VarDecl      -> this.asVarDecl()
        CXCursor_TypedefDecl  -> this.asTypedef()
        else -> "Expected toplevel declaration".left()
    })
    .map { it.withMeta(getMetadata()) }

/**
 * Collects metadata for a toplevel declaration cursor.
 */
fun CXCursor.getMetadata(): Meta {
    // FIXME? No doc available
    val comment  = clang_Cursor_getBriefCommentText(this).getOption()
    val extent   = getExtent()
    val location = extent.flatMap { it.asSourceRange() }
    val presumed = extent.flatMap { it.getStart().getPresumedLocation() }

    return Meta(
        location = location,
        presumedLocation = presumed,
        doc = comment
    )
}

fun CXCursor.getExtent(): Option<CXSourceRange> {
    val ptr = clang_getCursorExtent(this)
    return if (ptr.isNull) None else ptr.some()
}

fun CXSourceRange.getStart(): CXSourceLocation =
    clang_getRangeStart(this)

fun CXSourceRange.getEnd(): CXSourceLocation =
    clang_getRangeEnd(this)

fun CXSourceRange.asSourceRange(): Option<SourceRange> =
    getStart().asLocation().flatMap { begin ->
        getEnd().asLocation().map { end ->
            SourceRange(begin, end)
        }
    }

fun CXSourceLocation.asLocation(): Option<Location> {
    val line   = IntPointer(1)
    val col    = IntPointer(1)
    val offset = IntPointer(1)
    val file   = CXFile()

    return try {
        clang_getExpansionLocation(this, file, line, col, offset)
        line.getOption().flatMap { l ->
            col.getOption().map { c -> Location(Path.of(clang_getFileName(file).string), l, c) }
        }
    } finally {
        line.close(); col.close(); offset.close(); file.close()
    }
}

fun CXSourceLocation.getPresumedLocation(): Option<Location> {
    val line   = IntPointer(1)
    val col    = IntPointer(1)
    val offset = IntPointer(1)
    val file   = CXString()

    return try {
        clang_getPresumedLocation(this, file, line, col)
        line.getOption().flatMap { l ->
            col.getOption().map { c -> Location(Path.of(file.string), l, c) }
        }
    } finally {
        line.close(); col.close(); offset.close(); file.close()
    }
}


fun CXCursor.asTypedef(): Result<TopLevel.Typedef> =
    ifKind (CXCursor_TypedefDecl, "typedef") {
        clang_getTypedefDeclUnderlyingType(this).asType().map { type ->
            TopLevel.Typedef(clang_getTypedefName(clang_getCursorType(this)).string, type)
        }
    }

fun CXCursor.asStructDecl(): Result<TopLevel.Composite> =
    ifKind (CXCursor_StructDecl, "struct declaration") {
        TopLevel.Composite(
            this.spelling(),
            StructOrUnion.Struct,
            listOf() // TODO
        ).right()
    }

fun CXCursor.asVarDecl(): Result<TopLevel.Var> =
    ifKind (CXCursor_VarDecl, "variable declaration") {
        clang_getCursorType(this)
            .asType()
            .map { TopLevel.Var(this.spelling(), it) }
    }

fun CXCursor.getResultType(): Result<Type> {
    val typ = clang_getCursorResultType(this)
    return typ.asType()
}

fun CXCursor.getParameters(): Result<List<Param>> {
    val nargs = clang_Cursor_getNumArguments(this)
    return (0 until nargs)
        .map { clang_Cursor_getArgument(this, it) }
        .map { it.asParam() }
        .sequenceEither()
}

fun CXCursor.asFunctionDef(): Result<TopLevel.Fun> =
    asFunctionDecl().map { it.copy(definition = true)}

fun CXCursor.asFunctionDecl(): Result<TopLevel.Fun> =
    ifKind(CXCursor_FunctionDecl, "function declaration") {
        this.getResultType().flatMap { resultType ->
            this.getParameters().map { params ->
                /* TODO, fill in the constants */
                TopLevel.Fun(spelling(), false, resultType, params, false)
            }
        }
    }

fun CXCursor.asParam(): Result<Param> =
    ifKind(CXCursor_ParmDecl, "parameter declaration") {
        clang_getCursorType(this)
            .asType()
            .map { type -> Param(spelling(), type) }
    }

// CXType extensions

fun CXType.spelling(): String = clang_getTypeSpelling(this).string

/* Type declarations yield an assignable type */
fun CXCursor.asTypeDeclType(): Result<Type> =
    when(kind()) {
        CXCursor_EnumDecl   -> Type.Enum(spelling()).right()
        CXCursor_StructDecl -> Type.Struct(spelling()).right()
        CXCursor_UnionDecl  -> Type.Union(spelling()).right()
        else -> "Expected a type declaration, got ${kindName()}".left()
    }

/* Typedefs yield an assignable type */
fun CXCursor.asTypedefType(): Result<Type> =
    ifKind(CXCursor_TypedefDecl, "typedef") {
        clang_getTypedefDeclUnderlyingType(this).asType()
    }

fun CXType.asType(): Result<Type> =
    when (this.kind()) {
        CXType_Void -> Type.Void().right()
        CXType_Bool -> Type.Int(IKind.IBoolean).right()
        CXType_Char_U -> Type.Int(IKind.IUChar).right() // correct?
        CXType_UChar  -> Type.Int(IKind.IUChar).right() // correct?
        CXType_UShort  -> Type.Int(IKind.IUShort).right()
        CXType_UInt  -> Type.Int(IKind.IUInt).right()
        CXType_ULong  -> Type.Int(IKind.IULong).right()
        CXType_ULongLong  -> Type.Int(IKind.IULongLong).right()
        CXType_Char_S -> Type.Int(IKind.IChar).right() // correct?
        CXType_SChar -> Type.Int(IKind.ISChar).right() // correct?
        CXType_Short -> Type.Int(IKind.IShort).right()
        CXType_Int -> Type.Int(IKind.IInt).right()
        CXType_Long -> Type.Int(IKind.ILong).right()
        CXType_LongLong -> Type.Int(IKind.ILongLong).right()

        CXType_Float -> Type.Float(FKind.FFloat).right()
        CXType_Double -> Type.Float(FKind.FDouble).right()
        CXType_LongDouble -> Type.Float(FKind.FLongDouble).right()

        CXType_Pointer -> clang_getPointeeType(this).asType().map { Type.Ptr(it) }
        CXType_Record  -> Type.Struct(spelling()).right()
        CXType_Elaborated -> clang_getTypeDeclaration(this).asTypeDeclType()
        CXType_Enum    -> TODO()
        CXType_Typedef -> clang_getTypeDeclaration(this).asTypedefType().map { Type.Named(spelling(),it) }
        CXType_ConstantArray ->
            clang_getArrayElementType(this)
                .asType()
                .map { Type.Array(it, clang_getArraySize(this).some()) }
        CXType_IncompleteArray ->
            clang_getArrayElementType(this)
                .asType()
                .map { Type.Array(it) }

        // others that could occur in C?

        else -> "Could not parse type of kind '${this.kind()}'".left()
    }