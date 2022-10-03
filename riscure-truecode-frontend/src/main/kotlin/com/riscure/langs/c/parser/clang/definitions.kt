package com.riscure.langs.c.parser.clang

//fun CXCursor.getFunctionBody(): Result<Stmt> {
//    val children = this.children()
//    if (children.isEmpty()) {
//        return "Expected child representing function body, got nothing.".left()
//    }
//
//    return children.last().asStmt()
//}

//fun CXCursor.asStmt(): Result<Stmt> {
//    if (clang_isStatement(kind()) == 0) {
//        return "Expected statement, got cursor of kind ${kindName()}".left()
//    }
//
//    return when (kind()) {
//        CXCursor_DeclStmt -> asDecl()
//        CXCursor_CompoundStmt -> asBlock()
//        CXCursor_ReturnStmt -> asReturn()
//        else -> "Unrecognized statement of cursor kind ${kindName()}".left()
//    }
//}
//
//fun Int.isCTrue(): Boolean = this != 0
//fun Int.isCFalse(): Boolean = this == 0

//fun CXCursor.asReturn(): Result<Stmt.Return> = ifKind(CXCursor_ReturnStmt, "return statement") {
//    Stmt.Return().right() // TODO
// }

//fun CXCursor.asDecl(): Result<Stmt.Decl> = ifKind(CXCursor_DeclStmt, "declaration statement") {
//    val children = this.children()
//
//    if (children.size != 1) {
//        "Expected single declaration, got ${children.size} children.".left()
//    } else {
//        this.children()[0].asVarDecl()
//    }
//}
//
//fun CXCursor.asVarDecl(): Result<Stmt.Decl> = ifKind(CXCursor_VarDecl, "variable declaration") {
//    if (clang_isDeclaration(kind()).isCFalse()) {
//        "Expected declaration, got ${kindName()}".left()
//    } else if (clang_isInvalidDeclaration(this).isCTrue()) {
//        "Expected valid declaration; got partial AST.".left()
//    } else {
//        clang_getCursorType(this)
//            .asType()
//            .flatMap { type ->
//                // This is a bit odd;
//                // Sometimes we seem to have more than one child:
//                // e.g.: int x[4] = {1,2,3,4};
//                // has 2 children: one for the [4] and one for the init expression.
//                // but I have not found an API function that tells me how to parse the children.
//                val cs = children()
//                Optional.ofNullable(cs.lastOrNull())
//                    .asInitializer()
//                    .map { Stmt.Decl(this.spelling(), type, it) }
//        }
//    }
//}
//
//fun Optional<CXCursor>.asInitializer(): Result<Optional<Initializer>> {
//    return if (isEmpty) {
//        Optional.empty<Initializer>().right()
//    } else {
//        val cursor = get()
//        val kind = cursor.kind()
//        if (kind == CXCursor_InitListExpr) {
//            cursor.children()
//                .map { it.asExp() }
//                .sequenceEither()
//                .map { Optional.of(Initializer.InitArray(it)) }
//        } else if (clang_isExpression(kind) != 0) {
//            cursor
//                .asExp()
//                .map { Optional.of(Initializer.InitSingle(it)) }
//        } else {
//            "Could not parse initializer expression of kind ${get().kindName()}".left()
//        }
//    }
//}

//fun CXCursor.asInitArray(): Result<Initializer.InitArray> = TODO()
//fun CXCursor.asExp(): Result<Exp> = when (this.kind()) {
//    CXCursor_IntegerLiteral -> asInt()
//    CXCursor_CompoundLiteralExpr -> asCompoundLiteral()
//    else -> "Unrecognized expression of kind ${kindName()}".left()
//}
//
//fun CXCursor.asInt(): Result<Exp> = ifKind(CXCursor_IntegerLiteral, "integer literal") {
//    // TODO effective type
//    Exp.Const(Constant.CInt(42, IKind.IInt)).right()
//}
//
//fun CXCursor.asCompoundLiteral(): Result<Exp> = ifKind(CXCursor_CompoundLiteralExpr, "compound literal (union or struct)") {
//    TODO()
//}
//
//fun CXCursor.asBlock(): Result<Stmt.Block> = ifKind(CXCursor_CompoundStmt, "block") {
//    children()
//        .map { it.asStmt() }
//        .sequenceEither()
//        .map { Stmt.Block(it) }
//}
//
