/**
 * This file contains the translation from the libclang API to the
 * JVM AST representing a C translation unit.
 */
package com.riscure.langs.c.parser

import arrow.core.*
import com.riscure.getOption
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.TUID
import com.riscure.langs.c.parser.clang.*
import com.riscure.toBool
import org.bytedeco.javacpp.*
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*
import java.nio.file.Path

private typealias Result<T> = Either<String, T>

/**
 * Combinator to fail with a consistent message if we have an unexpected cursor kind.
 */
private fun <T> CXCursor.ifKind(k: Int, expectation: String, whenMatch: () -> Result<T>): Result<T> {
    if (kind() != k) {
        return "Expected ${expectation}. Got cursor of kind ${kindName()}".left()
    }

    return whenMatch()
}

fun CXCursor.asTranslationUnit(tuid: TUID): Result<TranslationUnit<CXCursor>> {
    if (this.kind() != CXCursor_TranslationUnit) {
        return "Expected translation unit, got cursor of kind ${this.kindName()}".left()
    }

    return this.children()
        // somehow e.g. an empty ';' results in top-level UnexposedDecls
        // not sure what else causes them.
        .filter { it.kind() != CXCursor_UnexposedDecl }
        .map { it.asDeclaration() }
        .sequence()
        .map { TranslationUnit(tuid, it) }
}

fun CXCursor.asDeclaration(): Result<Declaration<CXCursor>> =
    (when (kind()) {
        CXCursor_FunctionDecl ->
            if (children().any { child -> child.kind() == CXCursor_CompoundStmt })
                asFunctionDef()
            else asFunctionDecl()
        CXCursor_StructDecl   -> this.asStructDecl()
        CXCursor_UnionDecl    -> this.asUnionDecl()
        CXCursor_VarDecl      -> this.asVarDecl()
        CXCursor_TypedefDecl  -> this.asTypedef()
        CXCursor_EnumDecl     -> this.asEnumDecl()
        else -> "Expected toplevel declaration, got kind ${kindName()}".left()
    })
    .map {
        it.withMeta(getMetadata())
          .withStorage(getStorage())
    }

/**
 * Collects storage for a toplevel declaration cursor.
 */
fun CXCursor.getStorage(): Storage = clang_Cursor_getStorageClass(this).asStorage()
fun Int.asStorage(): Storage = when (this) {
    CX_SC_Static   -> Storage.Static
    CX_SC_Auto     -> Storage.Auto
    CX_SC_Extern   -> Storage.Extern
    CX_SC_Register -> Storage.Register
    // TODO default or report error?
    else           -> Storage.Default
}

/**
 * Collects metadata for a toplevel declaration cursor.
 */
fun CXCursor.getMetadata(): Meta {
    // FIXME? No doc available
    val comment  = clang_Cursor_getBriefCommentText(this).getOption()

    return Meta(
        location = getRange(),
        presumedLocation = getPresumedLocation(),
        doc = comment
    )
}

fun CXCursor.getStart(): Option<Location> =
    getExtent().flatMap {
        clang_getRangeStart(it).asLocation()
    }

fun CXCursor.getEnd(): Option<Location> =
    getExtent().flatMap {
        clang_getRangeEnd(it).asLocation()
    }

fun CXCursor.getRange(): Option<SourceRange> =
    getStart().flatMap { begin ->
        getEnd().map { end ->
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

fun CXCursor.getPresumedLocation(): Option<Location> {
    val line   = IntPointer(1)
    val col    = IntPointer(1)
    val offset = IntPointer(1)
    val file   = CXString()

    return try {
        getExtent()
            .flatMap {
                clang_getPresumedLocation(clang_getRangeStart(it), file, line, col)
                line.getOption().flatMap { l ->
                    col.getOption().map { c -> Location(Path.of(file.string), l, c) }
                }
            }
    } finally {
        line.close(); col.close(); offset.close(); file.close()
    }
}


fun CXCursor.asTypedef(): Result<Declaration.Typedef> =
    ifKind (CXCursor_TypedefDecl, "typedef") {
        clang_getTypedefDeclUnderlyingType(this).asType().map { type ->
            Declaration.Typedef(clang_getTypedefName(clang_getCursorType(this)).string, type)
        }
    }


fun CXCursor.asEnumDecl(): Result<Declaration.EnumDef> =
    ifKind (CXCursor_EnumDecl, "enum declaration") {
        Declaration.EnumDef(spelling(), listOf()).right() // TODO enumerators
    }

fun CXCursor.asStructDecl(): Result<Declaration.Composite> =
    ifKind (CXCursor_StructDecl, "struct declaration") {
        Declaration.Composite(
            this.spelling(),
            StructOrUnion.Struct,
            listOf() // TODO
        ).right()
    }

fun CXCursor.asUnionDecl(): Result<Declaration.Composite> =
    ifKind (CXCursor_UnionDecl, "union declaration") {
        Declaration.Composite(
            this.spelling(),
            StructOrUnion.Union,
            listOf() // TODO
        ).right()
    }

fun CXCursor.asVarDecl(): Result<Declaration.Var> =
    ifKind (CXCursor_VarDecl, "variable declaration") {
        clang_getCursorType(this)
            .asType()
            .map { Declaration.Var(this.spelling(), it, clang_isCursorDefinition(this).toBool()) }
    }

fun CXCursor.getReturnType(): Result<Type> {
    val typ = clang_getCursorResultType(this)
    return typ.asType()
}

fun CXCursor.getParameters(): Result<List<Param>> {
    val nargs = clang_Cursor_getNumArguments(this)
    return (0 until nargs)
        .map { clang_Cursor_getArgument(this, it) }
        .map { it.asParam() }
        .sequence()
}

fun CXCursor.asFunctionDef(): Result<Declaration.Fun<CXCursor>> =
    asFunctionDecl().map { it.copy(body = TODO())}

fun CXCursor.asFunctionDecl(): Result<Declaration.Fun<CXCursor>> =
    ifKind(CXCursor_FunctionDecl, "function declaration") {
        this.getReturnType().flatMap { resultType ->
            this.getParameters().map { params ->
                /* TODO, fill in the constants */
                Declaration.Fun(spelling(), false, resultType, params, false)
            }
        }
    }

fun CXCursor.asParam(): Result<Param> =
    ifKind(CXCursor_ParmDecl, "parameter declaration") {
        clang_getCursorType(this)
            .asType()
            .map { type -> Param(spelling(), type) }
    }

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
    when (kind()) {
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

        // http://clang.llvm.org/doxygen/classclang_1_1FunctionNoProtoType.html
        CXType_FunctionNoProto ->
            clang_getResultType(this)
                .asType()
                .map { retType -> Type.Fun(retType, listOf(), false) }
        CXType_FunctionProto   ->
        clang_getResultType(this)
            .asType()
            .flatMap { retType ->
                (0 until clang_getNumArgTypes(this))
                    .map { clang_getArgType(this, it).asType().map { type -> Param("", type) } }
                    .sequence()
                    .map { args -> Type.Fun(retType, args, false) }
            }

        // others that could occur in C?

        else -> "Could not parse type of kind '${kindName()}'".left()
    }