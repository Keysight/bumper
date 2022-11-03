package com.riscure.langs.c.parser.clang

import arrow.core.Either
import com.riscure.langs.c.ast.*
import com.riscure.langs.c.index.TUID
import org.bytedeco.llvm.clang.CXCursor
import org.bytedeco.llvm.clang.CXType

private typealias ParseResult<T> = Either<String, T>

interface ICursorParser {
    fun CXCursor.asTranslationUnit(tuid: TUID): ParseResult<TranslationUnit<CXCursor, CXCursor>>
    fun CXCursor.asDeclaration(): ParseResult<Declaration<CXCursor, CXCursor>>
    fun CXType.asType(): ParseResult<Type>
}