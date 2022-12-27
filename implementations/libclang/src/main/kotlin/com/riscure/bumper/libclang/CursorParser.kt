package com.riscure.bumper.libclang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.bumper.ast.*
import com.riscure.getOption
import com.riscure.bumper.index.Symbol
import com.riscure.bumper.index.TUID
import com.riscure.toBool
import org.bytedeco.javacpp.annotation.ByVal
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*

private typealias Result<T> = Either<String, T>
typealias CursorHash = Int

private object myDependencyOrder: Comparator<Pair<CXCursor, ClangDeclaration>> {
    override fun compare(d1: Pair<CXCursor, ClangDeclaration>, d2: Pair<CXCursor, ClangDeclaration>): Int =
        when (val l1 = d1.second.meta.location) {
            is None -> -1 // stable order
            is Some -> when (val l2 = d2.second.meta.location) {
                is None -> 1
                is Some -> dependencyOrder.compare(l1.value, l2.value) }
        }
}

data class UnitWithCursorData(
    val ast: TranslationUnit<CXCursor, CXCursor>,
    val elaboratedCursors: Map<CursorHash, ClangDeclaration>
)

/**
 * A stateful translation from libclang's CXCursors to our typed C ASTs.
 * The state is bound to a single translation unit, which is assumed to be parsed only once.
 *
 * This parser also elaborates inline definitions, rather than doing that in a separate pass.
 * The reason for this is that libclang does not allow us to distinguish inline definitions
 * from inline declarations. Types are treated semantically--rather than syntactically--in the
 * libclang API.
 */
open class CursorParser(
    val tuid: TUID,

    /**
     * Declarations that were referenced and need to be elaborated
     * to top-level definitions. Because we elaborate during parsing, we may
     * (re)name the declaration.
     *
     * We store in this map the symbol with the generated/elaborated name.
     * References should be consistently renamed to use the elaborated name.
     * This map is monotonically growing
     */
    private val elaborated    : MutableMap<CursorHash, Symbol> = mutableMapOf(),

    /**
     * A worklist for cursor representing (type) definitions that need to be elaborated.
     */
    private val toBeElaborated: MutableList<CXCursor>          = mutableListOf(),

    private var freshNameSuffix: Int = -1
) {

    /**
     * Generate a fresh name for an anonymous declaration.
     * The [hint] will be incorporated to give the user some idea of where this came from.
     */
    private fun freshAnonymousIdentifier(hint: Option<Ident> = None): Ident {
        freshNameSuffix += 1
        return "__anontype${hint.map { "_$it" }.getOrElse { "" }}_${freshNameSuffix}"
    }

    /**
     * Parse the list of [elaborated] definitions.
     * Every parsed definition is removed from that list.
     */
    private fun pleaseDoElaborate(done: Set<CursorHash>): Result<List<Pair<CXCursor, ClangDeclaration>>> {
        // We use [toBeElaborated] as a worklist
        // Parsing a declaration can add new items to the worklist as a side-effect.
        // When parsing fails, we immediately return the failure.
        return Either.catch({ it.message!! }) {
            val alreadyParsed = done.toMutableSet()
            val results = mutableListOf<Pair<CXCursor, ClangDeclaration>>()

            // work loop
            while (toBeElaborated.isNotEmpty()) {
                val cursor = toBeElaborated.removeAt(0)
                if (cursor.hash() in alreadyParsed) continue

                val decl = cursor
                    .asDeclaration()
                    .getOrHandle { throw Throwable(it) } // escalate

                alreadyParsed.add(cursor.hash())
                results.add(Pair(cursor, decl))
            }

            results
        }

    }

    // Parsing functions
    //-----------------------------------------------------------------------------------------------

    /**
     * Combinator to fail with a consistent message if we have an unexpected cursor kind.
     */
    private fun <T> CXCursor.ifKind(k: Int, expectation: String, whenMatch: () -> Result<T>): Result<T> {
        if (kind() != k) {
            return "Expected $expectation. Got cursor of kind ${kindName()}".left()
        }

        return whenMatch()
    }

    fun CXCursor.asTranslationUnit(): Result<UnitWithCursorData> =
        ifKind(CXCursor_TranslationUnit, "translation unit") {
            // We parse the top-level declarations
            val toplevelDecls = children()
                // somehow e.g. an empty ';' results in top-level UnexposedDecls
                .filter { cursor -> (cursor.kind() != CXCursor_UnexposedDecl) }
                .map    { cursor -> cursor.asDeclaration().map { d -> Pair(cursor, d) }}
                .sequence() // propagate errors

            toplevelDecls.flatMap { tlds ->
                // In addition, we elaborate some definitions that we encountered but perhaps did not yet
                // parse while parsing types.
                val done = tlds.map { it.first.hash() }.toMutableSet()
                val decls = pleaseDoElaborate(done).map { r -> tlds + r }

                // elaborate all the declarations
                decls
                    // we sort these, because we don't know from the traversal order where
                    // the elaborated declarations should appear. And the order is relevant for type completeness
                    // of structs and unions.
                    .map { it.sortedWith(myDependencyOrder) }
                    .flatMap { ds ->
                        ds
                            .map { (cursor, decl) ->
                                // rename the declaration from the programmer-written name to the elaborated name.
                                cursor
                                    .getSymbol()
                                    .map { Pair(cursor.hash(), decl.withIdent(it.name)) }
                            }
                            .sequence()
                    }
                    // and finally collect the outputs in a translation unit model.
                    .map { ds -> UnitWithCursorData(TranslationUnit(tuid, ds.map { it.second }), ds.toMap()) }
            }

            // FIXME this is unsound when we elaborate a named definition from a local scope,
            // because we are possibly extending the visibility of elaborated declarations,
            // which can now clash with an existing one.
            // We cannot fix that right now, because it requires renaming
            // all references. This is not a problem for any reference that we parse, but we do not
            // parse statements and expressions at the moment, so we cannot perform renaming there.
        }

    fun CXCursor.asDeclaration(): Result<ClangDeclaration> {
        val decl = when (kind()) {
            CXCursor_FunctionDecl ->
                if (children().any { child -> child.kind() == CXCursor_CompoundStmt })
                    asFunctionDef()
                else asFunctionDecl()
            CXCursor_StructDecl   -> this.asStruct()
            CXCursor_UnionDecl    -> this.elaborateUnion()
            CXCursor_VarDecl      -> this.asVariable()
            CXCursor_TypedefDecl  -> this.asTypedef()
            CXCursor_EnumDecl     -> this.asEnum()
            else                  -> "Expected toplevel declaration, got kind ${kindName()}".left()
        }

        // Fill in the properties of the Declaration class.
        return decl
            .map {
                it
                    .withMeta(getMetadata())
                    .withStorage(getStorage())
            }
    }

    /**
     * Check if this is a valid C identifier. Empty string is accepted.
     */
    private fun String.validateIdentifier(): Result<String> =
        this.right()
            // C identifiers start with a non-digit.
            // But an empty string is considered valid, regarding anonymous declarations.
            .filterOrOther({ Regex("[_a-zA-Z]?\\w*").matches(it) }) { s ->
                "Expected valid C identifier, got '$s'."
            }

    /**
     * Returns the spelling if it is a valid identifier, according to [validateIdentifier].
     */
    fun CXCursor.getIdentifier(): Result<String> =
        spelling()
            .validateIdentifier()

    /**
     * Collects storage for a toplevel declaration cursor.
     */
    fun CXCursor.getStorage(): Storage = clang_Cursor_getStorageClass(this).asStorage()

    /**
     * Map libclang's storage constants to our AST's enum values.
     */
    fun Int.asStorage(): Storage = when (this) {
        CX_SC_Static   -> Storage.Static
        CX_SC_Auto     -> Storage.Auto
        CX_SC_Extern   -> Storage.Extern
        CX_SC_Register -> Storage.Register
        else           -> Storage.Default
    }

    /**
     * Collects metadata for a toplevel declaration cursor.
     */
    fun CXCursor.getMetadata(): Meta {
        // FIXME? No doc available
        val comment = clang_Cursor_getBriefCommentText(this).getOption()

        return Meta(
            location = getRange(),
            presumedLocation = getPresumedLocation(),
            doc = comment
        )
    }

    fun CXCursor.asTypedef(): Result<Declaration.Typedef> =
        ifKind(CXCursor_TypedefDecl, "typedef") {
            clang_getTypedefDeclUnderlyingType(this)
                .asType()
                .flatMap { type ->
                    getIdentifier().map { id -> Declaration.Typedef(id, type) }
                }
        }

    fun CXCursor.asEnum(): Result<Declaration.Enum> =
        ifKind(CXCursor_EnumDecl, "enum declaration") {
            if (clang_isCursorDefinition(this).toBool()) {
                children()
                    .map { it.asEnumerator() }
                    .sequence()
                    .flatMap { enumerators ->
                        getIdentifier().map { id -> Declaration.Enum(id, enumerators.some()) }
                    }
            } else {
                getIdentifier().map { id -> Declaration.Enum(id, None)}
            }
        }

    fun CXCursor.asEnumerator(): Result<Enumerator> =
        ifKind(CXCursor_EnumConstantDecl, "enumerator") {
            val name  = spelling()
            val const = clang_getEnumConstantDeclValue(this)
            Enumerator(name, const).right()
        }

    private fun CXCursor.asComposite(): Result<Declaration.Composite> {
        // We check if this is the definition, because the field visitor
        // will just poke through the declaration into the related definition
        // and visit the fields there.
        val fields = if (clang_isCursorDefinition(this).toBool()) {
            type()
                .fields()
                .mapIndexed { i, it -> it.asField() }
                .sequence()
                .map { it.some() }
        } else {
            // For declarations, fields is None
            None.right()
        }

        return fields
            .flatMap { fs ->
                getIdentifier().map { id -> Declaration.Composite(id, StructOrUnion.Struct, fs) }
            }
    }

    fun CXCursor.asStruct(): Result<Declaration.Composite> =
        ifKind(CXCursor_StructDecl, "struct declaration") {
            asComposite()
                .map { c -> c.copy(structOrUnion = StructOrUnion.Struct) }
        }

    fun CXCursor.elaborateUnion(): Result<Declaration.Composite> =
        ifKind(CXCursor_UnionDecl, "union declaration") {
            asComposite()
                .map { c -> c.copy(structOrUnion = StructOrUnion.Union) }
        }

    fun CXType.fields(): List<CXCursor> {
        val ts = mutableListOf<CXCursor>()
        val wrapped = object : CXFieldVisitor() {
            override fun call(@ByVal field: CXCursor?, p2: CXClientData?): Int {
                ts.add(field!!)
                return CXVisit_Continue
            }
        }

        try {
            clang_Type_visitFields(this, wrapped, null)
        } finally {
            wrapped.deallocate()
        }

        return ts
    }

    fun CXCursor.asField(): Result<Field> {
        return type()
            .asFieldType()
            .flatMap { type ->
                val attrs = this.cursorAttributes(type())
                getIdentifier()
                    .map { id ->
                        Field(
                            id,
                            type.withAttrs(type.attrs + attrs),
                            clang_getFieldDeclBitWidth(this).let { if (it == -1) None else Some(it) }
                        )
                    }
            }
    }

    fun CXCursor.asVariable(): Result<Declaration.Var<CXCursor>> =
        ifKind(CXCursor_VarDecl, "variable declaration") {
            type()
                .asType()
                .flatMap { type ->
                    val rhs: Option<Result<CXCursor>> = if (clang_isCursorDefinition(this).toBool()) {
                        val subexps = this.children().filter { clang_isExpression(it.kind()).toBool() }
                        when {
                            // most global variable initializations are of the form `<typed name> = <exp>`
                            subexps.size == 1                                               ->
                                subexps[0].right().some()
                            // an array can be initialized as xs[size] = expr,
                            // in which case we have two sub-expressions, and the last one represents the rhs.
                            type is Type.Array && type.size.isDefined() && subexps.size > 1 ->
                                subexps.last().right().some()
                            // some other form that has a differently shaped AST that we have not forseen
                            else                                                            ->
                                "Failed to extract right-hand side of variable declaration '${spelling()}'.".left()
                                    .some()
                        }
                    } else None

                    rhs.sequenceEither()
                        .flatMap { def ->
                            getIdentifier().map { id -> Declaration.Var(id, type, def) }
                        }
                }
        }

    fun CXCursor.getReturnType(): Result<Type> {
        val typ = clang_getCursorResultType(this)
        return typ.asType()
    }

    fun CXCursor.getParameters(): Result<List<Param>> {
        val nargs = clang_Cursor_getNumArguments(this)
        return (0 until nargs)
            .map { clang_Cursor_getArgument(this, it).asParam() }
            .sequence()
    }

    fun CXCursor.asFunctionDef(): Result<Declaration.Fun<CXCursor>> =
        asFunctionDecl().flatMap { decl ->
            Either
                .fromNullable(children().find { clang_isStatement(it.kind()).toBool() })
                .mapLeft { "Could not parse function body of ${decl.ident}." }
                .map { decl.copy(body = it.some()) }
        }

    fun CXCursor.asFunctionDecl(): Result<Declaration.Fun<CXCursor>> =
        ifKind(CXCursor_FunctionDecl, "function declaration") {
            getReturnType().flatMap { resultType ->
                getParameters().flatMap { params ->
                    getIdentifier()
                        .filterOrOther({ it != "" }) { "Anonymous function declarations are not allowed." }
                        .map { id ->
                            Declaration.Fun(
                                id,
                                clang_Cursor_isFunctionInlined(this).toBool(),
                                resultType,
                                params,
                                clang_Cursor_isVariadic(this).toBool(),
                            )
                        }
                }
            }
        }

    fun CXCursor.asParam(): Result<Param> =
        ifKind(CXCursor_ParmDecl, "parameter declaration") {
            type()
                .asType()
                .flatMap { type ->
                    getIdentifier()
                        .map { id -> Param(id, type) }
                }
        }

    /**
     * Get the entity kind for the declaration/definition under the cursor.
     */
    fun CXCursor.getEntityKind(): Result<EntityKind> = when (kind()) {
        CXCursor_StructDecl   -> EntityKind.Struct.right()
        CXCursor_UnionDecl    -> EntityKind.Union.right()
        CXCursor_EnumDecl     -> EntityKind.Enum.right()
        CXCursor_FunctionDecl -> EntityKind.Fun.right()
        CXCursor_VarDecl      -> EntityKind.Var.right()
        CXCursor_TypedefDecl  -> EntityKind.Typedef.right()
        else                  -> "Failed to parse entity kind from cursor kind '${kindName()}'".left()
    }

    fun CXCursor.getElaboratedSymbol(): Result<Symbol> =
        Either
            .fromNullable(elaborated[hash()])
            .mapLeft { "Failed to get elaborated name for cursor." }

    /**
     * Return a symbol for the definition under the cursor, elaborating one if necessary.
     */
    fun CXCursor.getSymbol(): Result<Symbol> =
        // elaboration may have already (re)named the definition under the cursor,
        // so we check our tables
        getElaboratedSymbol()
            // if not, we will generate one now,
            // and record the generated symbol.
            .handleErrorWith {
                generateSymbol()
                    .tap { sym ->
                        assert(hash() !in elaborated)

                        // We remember that we chose a name for this definition,
                        elaborated[hash()] = sym
                        // and we mark the cursor for elaboration.
                        toBeElaborated.add(this)
                    }
            }

    fun CXCursor.generateSymbol(): Result<Symbol> =
        // get what the programmer wrote.
        getEntityKind()
            .zip(getIdentifier())
            .map { (kind, id) ->
                // if the definition is anonymous, we generate a fresh name for it
                val ident = if (id == "") freshAnonymousIdentifier() else id
                Symbol(tuid, TLID(ident, kind))
            }

    /**
     * Field types are treated a little different,
     * because anonymouse inline struct/union type definitions need
     * not to be elaborated. That would change the visibility of the
     * nested members.
     */
    fun CXType.asFieldType(): Result<FieldType> =
        when (kind()) {
            // anonymous union/struct field are treated differently
            CXType_Record -> {
                clang_getTypeDeclaration(this)
                    .asDeclaration()
                    .flatMap { d ->
                        // sanity check
                        assert(d.ident == "")
                        when (d) {
                            is Declaration.Composite -> FieldType.AnonComposite(d.structOrUnion, d.fields).right()
                            else -> "Invariant violation: failed to parse anonymous field.".left()
                        }
                    }
            }
            else -> asType()
        }

    fun CXType.asType(): Result<Type> =
        when (kind()) {
            CXType_Void            -> Type.Void().right()
            CXType_Bool            -> Type.Int(IKind.IBoolean).right()
            CXType_Char_U          -> Type.Int(IKind.IUChar).right()
            CXType_UChar           -> Type.Int(IKind.IUChar).right()
            CXType_UShort          -> Type.Int(IKind.IUShort).right()
            CXType_UInt            -> Type.Int(IKind.IUInt).right()
            CXType_ULong           -> Type.Int(IKind.IULong).right()
            CXType_ULongLong       -> Type.Int(IKind.IULongLong).right()
            CXType_Char_S          -> Type.Int(IKind.IChar).right()
            CXType_SChar           -> Type.Int(IKind.ISChar).right()
            CXType_Short           -> Type.Int(IKind.IShort).right()
            CXType_Int             -> Type.Int(IKind.IInt).right()
            CXType_Long            -> Type.Int(IKind.ILong).right()
            CXType_LongLong        -> Type.Int(IKind.ILongLong).right()
            CXType_Float           -> Type.Float(FKind.FFloat).right()
            CXType_Double          -> Type.Float(FKind.FDouble).right()
            CXType_LongDouble      -> Type.Float(FKind.FLongDouble).right()
            CXType_Complex         ->
                clang_getElementType(this)
                    .asType()
                    .flatMap {
                        when (it) {
                            is Type.Float -> Type.Complex(it.kind).right()
                            else          -> "Complex element type is not a float.".left()
                        }
                    }

            CXType_Pointer         ->
                clang_getPointeeType(this)
                    .asType()
                    .map { Type.Ptr(it) }

            CXType_Typedef         -> {
                val cursor = clang_getTypeDeclaration(this)
                cursor
                    .getIdentifier()
                    .flatMap { id ->
                        // clang presents this as a typedef.
                        if (id == "__builtin_va_list") {
                            Type.VaList().right()
                        } else {
                            cursor
                                .getSymbol()
                                .map { Type.Typedeffed(it) }
                        }
                    }
            }

            CXType_ConstantArray   ->
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
                            .map { i ->
                                clang_getArgType(this, i)
                                    .asType()
                                    .map { type -> Param("", type) }
                            }
                            .sequence()
                            .map { args -> Type.Fun(retType, args, false) }
                    }

            CXType_Atomic          ->
                clang_Type_getValueType(this)
                    .asType()
                    .map { Type.Atomic(it) }

            // Special type kind for inline declarations are elaborated by clang.
            CXType_Elaborated      ->
                clang_Type_getNamedType(this)
                    .asType()

            CXType_Record          -> {
                clang_getTypeDeclaration(this)
                    .getSymbol()
                    .flatMap { sym ->
                        when (sym.kind) {
                            EntityKind.Struct -> Type.Struct(sym).right()
                            EntityKind.Union  -> Type.Union(sym).right()
                            else              -> "Invariant violation: failed to reference elaborated struct/union.".left()
                        }
                    }
            }

            CXType_Enum            -> {
                clang_getTypeDeclaration(this)
                    .getSymbol()
                    .map { sym -> Type.Enum(sym) }
            }

            CXType_Unexposed       -> {
                // Some types that libclang keeps symbolic it does not expose via the libclang interface.
                // In particular: typeof expressions.
                // But, we can try to get a semantically equivalent type at this point from libclang
                // and parse that instead:
                clang_getCanonicalType(this).asType()
            }

            // There are others, but as far as I know, these are non-C types.
            else                   -> "Could not parse type of kind '${kindName()}'".left()
        }.map { type -> type.withAttrs(getTypeAttrs()) }

    fun CXType.getTypeAttrs(): Attrs {
        val attrs = mutableListOf<Attr>()
        if (clang_isVolatileQualifiedType(this).toBool())
            attrs.add(Attr.Volatile)
        if (clang_isRestrictQualifiedType(this).toBool())
            attrs.add(Attr.Restrict)
        if (clang_isConstQualifiedType(this).toBool())
            attrs.add(Attr.Constant)
        return attrs
    }

    fun CXCursor.cursorAttributes(type: CXType): Attrs =
        fold(monoid = Monoid.list(), true) {
            if (clang_isAttribute(kind()).toBool()) {
                asAttribute(type).orNone().toList()
            } else listOf()
        }

    fun CXCursor.asAttribute(type: CXType): Result<Attr> = when (kind()) {
        CXCursor_ConstAttr   -> Attr.Constant.right()
        CXCursor_AlignedAttr ->
            type.getAlignment()
                .toEither { "Could not get alignment for type with alignment attribute." }
                .map { Attr.AlignAs(it) }

        /* TODO
    CXCursor_UnexposedAttr  -> TODO()
    CXCursor_AnnotateAttr   -> TODO()
    CXCursor_AsmLabelAttr   -> TODO()
    CXCursor_PackedAttr     -> TODO()
    CXCursor_PureAttr       -> TODO()
    CXCursor_NoDuplicateAttr -> TODO()
    CXCursor_VisibilityAttr -> TODO()
    CXCursor_ConvergentAttr -> TODO()
    CXCursor_WarnUnusedAttr -> TODO()
    CXCursor_WarnUnusedResultAttr -> TODO()
    */

        else                 -> "Not a recognized attribute?".left()
    }

    fun CXType.getAlignment(): Option<Long> {
        val align = clang_Type_getAlignOf(this)
        return when {
            align < 0 -> none()
            else      -> align.some()
        }
    }
}