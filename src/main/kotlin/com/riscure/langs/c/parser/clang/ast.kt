/**
 * This file contains the translation from the libclang API to the
 * JVM AST representing a C translation unit.
 */
package com.riscure.langs.c.parser

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.getOption
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.TUID
import com.riscure.langs.c.parser.clang.*
import com.riscure.toBool
import org.bytedeco.javacpp.*
import org.bytedeco.javacpp.annotation.ByVal
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang
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

fun CXCursor.asTranslationUnit(tuid: TUID): Result<TranslationUnit> {
    if (this.kind() != CXCursor_TranslationUnit) {
        return "Expected translation unit, got cursor of kind ${this.kindName()}".left()
    }

    return this.children()
        .filter { cursor -> when {
            // somehow e.g. an empty ';' results in top-level UnexposedDecls
            // not sure what else causes them.
            cursor.kind() == CXCursor_UnexposedDecl -> false
            // Clang expands typedef struct {..} to two top-level declarations, one of which is anonymous.
            cursor.isAnonymousDeclaration()         -> false
            else                                    -> true
        }}
        .map { it.asTopLevel() }
        .sequence()
        .map { TranslationUnit(tuid, it) }
}

fun CXCursor.isAnonymousDeclaration(): Boolean =
    // clang_isAnonymous and related don't work as expected
    kind() in listOf(CXCursor_EnumDecl, CXCursor_StructDecl, CXCursor_UnionDecl)
        && spelling().isEmpty()

fun CXCursor.asTopLevel(): Result<TopLevel> =
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


fun CXCursor.asTypedef(): Result<TopLevel.Typedef> =
    ifKind (CXCursor_TypedefDecl, "typedef") {
        clang_getTypedefDeclUnderlyingType(this).asType().map { type ->
            TopLevel.Typedef(clang_getTypedefName(clang_getCursorType(this)).string, type)
        }
    }


fun CXCursor.asEnumDecl(): Result<TopLevel.EnumDef> =
    ifKind (CXCursor_EnumDecl, "enum declaration") {
        TopLevel.EnumDef(spelling(), listOf()).right() // TODO enumerators
    }

fun CXCursor.asStructDecl(): Result<TopLevel.Composite> =
    ifKind (CXCursor_StructDecl, "struct declaration") {
        clang_getCursorType(this)
            .fields()
            .map { it.asField() }
            .sequence()
            .map { fields -> TopLevel.Composite(this.spelling(), StructOrUnion.Struct, fields) }
    }

fun CXType.fields(): List<CXCursor> {
    val ts = mutableListOf<CXCursor>()
    val wrapped = object: CXFieldVisitor() {
        override fun call(@ByVal field: CXCursor?, p2: CXClientData?): Int {
            ts.add(field!!)
            return CXVisit_Continue
        }
    }

    clang_Type_visitFields(this, wrapped, null)
    wrapped.deallocate()

    return ts
}

fun CXCursor.asField(): Result<Field> =
    clang_getCursorType(this)
        .asType()
        .map { type ->
            Field(
                spelling(),
                type,
                clang_getFieldDeclBitWidth(this).let { if (it == -1) None else Some(it) },
                spelling().isEmpty() // TODO correct?
            )
        }

fun CXCursor.asUnionDecl(): Result<TopLevel.Composite> =
    ifKind (CXCursor_UnionDecl, "union declaration") {
        TopLevel.Composite(
            this.spelling(),
            StructOrUnion.Union,
            listOf() // TODO
        ).right()
    }

fun CXCursor.asVarDecl(): Result<TopLevel.Var> =
    ifKind (CXCursor_VarDecl, "variable declaration") {
        clang_getCursorType(this)
            .asType()
            .map { TopLevel.Var(this.spelling(), it, clang_isCursorDefinition(this).toBool()) }
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

fun CXCursor.asFunctionDef(): Result<TopLevel.Fun> =
    asFunctionDecl().map { it.copy(isDefinition = true)}

fun CXCursor.asFunctionDecl(): Result<TopLevel.Fun> =
    ifKind(CXCursor_FunctionDecl, "function declaration") {
        this.getReturnType().flatMap { resultType ->
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

fun CXCursor.asTypeDeclType(): Result<Type> =
    when(kind()) {
        CXCursor_EnumDecl   -> asEnumDecl().map   { Type.InlineCompound(it) }
        CXCursor_StructDecl -> asStructDecl().map { Type.InlineCompound(it) }
        CXCursor_UnionDecl  -> asUnionDecl().map  { Type.InlineCompound(it) }
        else -> "Expected a compound type declaration, got ${kindName()}".left()
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

        // Special type kind for inline declarations.
        // Clang elaborated the inline declaration to a top-level entity.
        // We represent it inline, so that pretty-printing recovers the original,
        // and we do not expose new names after pretty-printing.
        // The elaborated declaration can have a name! If the anonymous declaration is in a function parameter,
        // the name is not visible outside of the function body.
        CXType_Elaborated -> clang_getTypeDeclaration(this).asTypeDeclType()

        // others that could occur in C?

        else -> "Could not parse type of kind '${kindName()}'".left()
    }