package com.riscure.langs.c.parser.clang

import arrow.core.*
import arrow.typeclasses.Monoid
import com.riscure.getOption
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.TUID
import com.riscure.toBool
import org.bytedeco.javacpp.annotation.ByVal
import org.bytedeco.llvm.clang.*
import org.bytedeco.llvm.global.clang.*

private typealias Result<T> = Either<String, T>
typealias ClangDeclaration = Declaration<CXCursor, CXCursor>
typealias CursorHash = Int

/**
 * A stateful translation from libclang's CXCursors to our typed C ASTs.
 * The state is bound to a single translation unit.
 */
open class CursorParser(
    val tuid: TUID,

    /**
     * Mapping cursors of declarations to the parsed declarations.
     */
    protected val declarationTable: MutableMap<CursorHash, ClangDeclaration> = mutableMapOf(),

    /**
     * A mapping of declarations to the cursor identifier of their corresponding definitions.
     */
    protected val resolutionTable: MutableMap<ErasedDeclaration, CursorHash> = mutableMapOf(),
) {

    // Parser state management
    //-----------------------------------------------------------------------------------------------

    val declarations: Map<CursorHash, ClangDeclaration> get() = declarationTable
    val resolutions : Map<ErasedDeclaration, CursorHash> get() = resolutionTable

    /**
     * Record a parsed declaration in the parse state tables.
     */
    private fun <T:ClangDeclaration> CXCursor.memoize(parse: () -> Result<T>): Result<T> {
        val id = this.hash()
        val result = parse.invoke()

        return result.tap { decl ->
            declarationTable[id] = decl

            // Ask clang if this declaration has a definition.
            // Store the cursor hash of the definition for now.
            // This will allow us to find the corresponding ClangDeclaration from declarationTable
            // once we parsed the entire translation unit. At this moment the definition might not yet
            // been seen, because the declaration can be a forward declaration.
            clang_getCursorDefinition(this)
                .filterNullCursor()
                .tap { resolutionTable[decl] = it.hash() }
        }
    }

    /**
     * Get the table mapping declarations to definitions from the parser state.
     */
    private fun getDefinitions(): Result<Map<ErasedDeclaration, ClangDeclaration>> =
        Either.catch({"Failed to resolve declaration to corresponding definition."}) {
            resolutionTable.mapValues { entry ->
                when (val def = declarationTable[entry.value].toOption()) {
                    is None -> throw RuntimeException()
                    is Some -> def.value
                }
            }
        }

    // Parsing functions
    //-----------------------------------------------------------------------------------------------

    /**
     * Combinator to fail with a consistent message if we have an unexpected cursor kind.
     */
    private fun <T> CXCursor.ifKind(k: Int, expectation: String, whenMatch: () -> Result<T>): Result<T> {
        if (kind() != k) {
            return "Expected ${expectation}. Got cursor of kind ${kindName()}".left()
        }

        return whenMatch()
    }

    fun CXCursor.asTranslationUnit(): Result<TranslationUnit<CXCursor, CXCursor>> =
        ifKind(CXCursor_TranslationUnit, "translation unit") {
            children()
                .filter { cursor ->
                    when {
                        // somehow e.g. an empty ';' results in top-level UnexposedDecls
                        // not sure what else causes them.
                        cursor.kind() == CXCursor_UnexposedDecl -> false
                        else                                    -> true
                    }
                }
                .mapIndexed { index, it ->
                    // Each declaration is passed the declaration site,
                    // represented by the index of the child.
                    // This index is stable in case we reparse the same translation unit.
                    Site.root.scope(Site.Toplevel(index)) { site ->
                        it.asDeclaration(site)
                    }
                }
                .sequence()
                .flatMap { toplevelDecls ->
                    val unitDeclarations = declarations.values
                        .filter { it.visibility == Visibility.TUnit }

                    getDefinitions()
                        .map {
                            TranslationUnit(tuid, toplevelDecls, unitDeclarations, it)
                        }
                }
        }

    fun CXCursor.asDeclaration(site: Site): Result<Declaration<CXCursor, CXCursor>> {
        // We might as well check if we already parsed this one:
        declarationTable[this.hash()].toOption().tap { return it.right() }

        // If not, lets go and parse it:
        val decl = when (kind()) {
            CXCursor_FunctionDecl ->
                if (children().any { child -> child.kind() == CXCursor_CompoundStmt })
                    asFunctionDef(site)
                else asFunctionDecl(site)
            CXCursor_StructDecl   -> this.asStructDecl(site)
            CXCursor_UnionDecl    -> this.asUnionDecl(site)
            CXCursor_VarDecl      -> this.asVarDecl(site)
            CXCursor_TypedefDecl  -> this.asTypedef(site)
            CXCursor_EnumDecl     -> this.asEnumDecl(site)
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

    private fun String.validateIdentifier(): Option<String> =
        this.some().filter { Regex("[_a-zA-Z]\\w*").matches(it) }

    fun CXCursor.getIdentifier(): Option<String> = spelling().validateIdentifier()

    /**
     * Collects storage for a toplevel declaration cursor.
     */
    fun CXCursor.getStorage(): Storage = clang_Cursor_getStorageClass(this).asStorage()
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

    fun CXCursor.asTypedef(site: Site): Result<Declaration.Typedef> = memoize {
        ifKind(CXCursor_TypedefDecl, "typedef") {
            site.scope(Site.Typedef) { typedefSite ->
                clang_getTypedefDeclUnderlyingType(this)
                    .asType(typedefSite)
                    .map { type ->
                        Declaration.Typedef(site, clang_getTypedefName(type()).string.validateIdentifier(), type)
                    }
            }
        }
    }

    fun CXCursor.asEnumDecl(site: Site): Result<Declaration.Enum> = memoize {
        ifKind(CXCursor_EnumDecl, "enum declaration") {
            Declaration.Enum(site, getIdentifier(), None).right() // TODO enumerators
        }
    }

    private fun CXCursor.asComposite(site: Site): Result<Declaration.Composite> {
        // We check if this is the definition.
        val fields = if (clang_isCursorDefinition(this).toBool()) {
            type()
                .fields()
                .mapIndexed { i, it -> it.asField(site + Site.Member(i)) }
                .sequence()
                .map { it.some() }
        } else {
            // For declarations, fields is None
            None.right()
        }

        return fields
            .map { fs -> Declaration.Composite(site, getIdentifier(), StructOrUnion.Struct, fs) }
    }

    fun CXCursor.asStructDecl(site: Site): Result<Declaration.Composite> = memoize {
        ifKind(CXCursor_StructDecl, "struct declaration") {
            asComposite(site).map { c -> c.copy(structOrUnion = StructOrUnion.Struct) }
        }
    }

    fun CXCursor.asUnionDecl(site: Site): Result<Declaration.Composite> = memoize {
        ifKind(CXCursor_UnionDecl, "union declaration") {
            asComposite(site).map { c -> c.copy(structOrUnion = StructOrUnion.Union) }
        }
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

    /**
     * @param site depicts the declaration site at which the surrounding composite is parsed.
     */
    fun CXCursor.asField(site: Site): Result<Field> {
        return type()
            .asType(site)
            .map { type ->
                val attrs = this.cursorAttributes(type())
                Field(
                    site,
                    getIdentifier(),
                    type.withAttrs(type.attrs + attrs),
                    clang_getFieldDeclBitWidth(this).let { if (it == -1) None else Some(it) }
                )
            }
    }

    fun CXCursor.asVarDecl(site: Site): Result<Declaration.Var<CXCursor>> = memoize {
        ifKind(CXCursor_VarDecl, "variable declaration") {
            site.scope(Site.VarType) { typeSite ->
                type()
                    .asType(typeSite)
                    .flatMap { type ->
                        val rhs: Option<Result<CXCursor>> = if (clang_isCursorDefinition(this).toBool()) {
                            val subexps = this.children().filter { clang_isExpression(it.kind()).toBool() }
                            if (subexps.size != 1) {
                                "Failed to extract right-hand side of variable declaration.".left().some()
                            } else subexps[0].right().some()
                        } else None

                        rhs.sequenceEither()
                            .map { def -> Declaration.Var(site, this.spelling(), type, def) }
                    }
            }
        }
    }

    /**
     * @param site is the site of the surrounding function declaration
     */
    fun CXCursor.getReturnType(surrounding: Site): Result<Type> =
        surrounding.scope(Site.FunctionReturn) { retSite ->
            val typ = clang_getCursorResultType(this)
            typ.asType(retSite)
        }

    fun CXCursor.getParameters(site: Site): Result<List<Param>> {
        val nargs = clang_Cursor_getNumArguments(this)
        return (0 until nargs)
            .map { clang_Cursor_getArgument(this, it) }
            .mapIndexed { i, c -> c.asParam(site + Site.FunctionParam(i)) }
            .sequence()
    }

    fun CXCursor.asFunctionDef(site: Site): Result<Declaration.Fun<CXCursor>> = memoize {
        asFunctionDecl(site).flatMap { decl ->
            Either
                .fromNullable(children().find { clang_isStatement(it.kind()).toBool() })
                .mapLeft { "Could not parse function body of ${decl.name}." }
                .map { decl.copy(body = it.some()) }
        }
    }

    fun CXCursor.asFunctionDecl(site: Site): Result<Declaration.Fun<CXCursor>> = memoize {
        ifKind(CXCursor_FunctionDecl, "function declaration") {
            getReturnType(site).flatMap { resultType ->
                getParameters(site).map { params ->
                    Declaration.Fun(
                        site,
                        spelling(),
                        clang_Cursor_isFunctionInlined(this).toBool(),
                        resultType,
                        params,
                        clang_Cursor_isVariadic(this).toBool(),
                    )
                }
            }
        }
    }

    fun CXCursor.asParam(site: Site): Result<Param> =
        ifKind(CXCursor_ParmDecl, "parameter declaration") {
            type()
                .asType(site)
                .map { type ->
                    Param(site, getIdentifier(), type)
                }
        }

    /**
     * When 'struct S' or 'struct S { ... }' or similar appear as types,
     * Libclang calls this an 'elaborated type'. Probably because Clang actually hoists inline
     * definitions to top-level nodes in its internal AST.
     */
    fun CXCursor.asElaboratedType(site: Site): Result<Type> =
        when (kind()) {
            CXCursor_EnumDecl   -> asEnumDecl(site).map   { Type.InlineDeclaration(it) }
            CXCursor_StructDecl -> asStructDecl(site).map { Type.InlineDeclaration(it) }
            CXCursor_UnionDecl  -> asUnionDecl(site).map  { Type.InlineDeclaration(it) }
            else                -> "Expected a compound type declaration, got ${kindName()}".left()
        }

    fun CXCursor.asAnonymousMemberType(site: Site): Result<Type> =
        when (kind()) {
            CXCursor_UnionDecl  -> asUnionDecl(site).map  { Type.InlineDeclaration(it) }
            CXCursor_StructDecl -> asStructDecl(site).map { Type.InlineDeclaration(it) }
            else                -> "Expected a compound type declaration, got ${kindName()}".left()
        }

    /**
     * Return a reference to the definition under the cursor.
     */
    fun CXCursor.getRef(byName: String): Result<Ref> {
        // first sanity check if this is indeed a definition
        if (!clang_isCursorDefinition(this).toBool()) { return "Expected definition, to ${kindName()}.".left() }

        // Get the declaration's symbol.
        // In C a reference should always resolve by a previous declaration, so this should resolve already.
        return when (val sym = declarationTable[this.hash()].toOption().flatMap { it.mkSymbol(tuid) }) {
            is None -> "Failed to resolve name $byName to declaration".left()
            is Some -> Ref(byName, sym.value).right()
        }

    }

    fun CXType.asRef(): Result<Ref> = clang_getTypeDeclaration(this).getRef(spelling())

    fun CXType.asType(site: Site): Result<Type> =
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
                    .asType(site)
                    .flatMap {
                        when (it) {
                            is Type.Float -> Type.Complex(it.kind).right()
                            else          -> "Complex element type is not a float.".left()
                        }
                    }

            CXType_Pointer         ->
                site.scope(Site.Pointee) { pointeeSite ->
                    clang_getPointeeType(this)
                        .asType(pointeeSite)
                        .map { Type.Ptr(it) }
                }

            CXType_Typedef         ->
                asRef().map { Type.Typedeffed(it) }

            CXType_ConstantArray   ->
                clang_getArrayElementType(this)
                    .asType(site)
                    .map { Type.Array(it, clang_getArraySize(this).some()) }

            CXType_IncompleteArray ->
                clang_getArrayElementType(this)
                    .asType(site)
                    .map { Type.Array(it) }

            // http://clang.llvm.org/doxygen/classclang_1_1FunctionNoProtoType.html
            CXType_FunctionNoProto ->
                clang_getResultType(this)
                    .asType(site)
                    .map { retType -> Type.Fun(retType, listOf(), false) }

            CXType_FunctionProto   ->
                clang_getResultType(this)
                    .asType(site)
                    .flatMap { retType ->
                        (0 until clang_getNumArgTypes(this))
                            .map { i ->
                                val argSite = site + Site.FunctionParam(i)
                                clang_getArgType(this, i)
                                    .asType(argSite)
                                    .map { type -> Param(argSite, None, type) }
                            }
                            .sequence()
                            .map { args -> Type.Fun(retType, args, false) }
                    }

            CXType_Atomic          ->
                clang_Type_getValueType(this)
                    .asType(site)
                    .map { Type.Atomic(it) }

            // Special type kind for inline declarations.
            // Any named struct or union type is actually an inline declaration or definition.
            CXType_Elaborated      ->
                clang_getTypeDeclaration(this)
                    .asElaboratedType(site)

            // This is the kind of *anonymous* struct/union types.
            // Apparently added in C11
            CXType_Record ->
                clang_getTypeDeclaration(this)
                    .asAnonymousMemberType(site)

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