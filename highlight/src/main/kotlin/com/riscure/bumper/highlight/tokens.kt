/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

data class Position(
    val line: Int,
    val column: Int
)

enum class StrEncoding {
    None,
    Wide,
    U16,
    U32,
    UTF8,
    RAW;

    fun pp() = when (this) {
        None -> ""
        Wide -> "L"
        U16  -> "u"
        U32  -> "U"
        UTF8 -> "u8"
        RAW  -> "R"
    }

    companion object {
        @JvmStatic
        fun parse(encoding: String) = when (encoding) {
            ""   -> None
            "L"  -> Wide
            "u"  -> U16
            "U"  -> U32
            "u8" -> UTF8
            "R"  -> RAW
            else -> throw RuntimeException("Not a valid character encoding $encoding")
        }
    }
}

enum class Keywords {
    Sizeof,
    AlignAs,
    AlignOf,
    Bool,
    Generic,
    Complex,
    Imaginary,
    StaticAssert,
    Asm,
    Attribute,
    VaArg,
    OffsetOf,
    Const,
    Inline,
    Packed,
    Restrict,
    Volatile,
    Auto,
    Break,
    Case,
    Continue,
    Default,
    Do,
    Else,
    Enum,
    Extern,
    For,
    Goto,
    If,
    NoReturn,
    Register,
    Return,
    Signed,
    Unsigned,
    Static,
    Struct,
    Switch,
    Typedef,
    Union,
    While
}

sealed interface Token {

    fun pp(): String

    val position: Position

    data class Directive(
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

    data class Identifier(
        val ident: String,
        override val position: Position
    ): Token {
        override fun pp(): String = ident
    }

    data class Type(
        val lexeme: String,
        override val position: Position,
    ): Token {
        override fun pp(): String = lexeme
    }

    data class Keyword(
        val word: Keywords,
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

//    data class Operator(
//        val op: Op,
//        val position: Position,
//    ): Token

    data class Punctuation(
        val lexeme: String,
        override val position: Position,
    ): Token {
        override fun pp(): String = lexeme
    }

    data class CharLiteral(
        val encoding: StrEncoding,
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = "${encoding.pp()}'$lexeme'"
    }

    data class StringLiteral(
        val encoding: StrEncoding,
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = "${encoding.pp()}\"$lexeme\""
    }

    data class IntLiteral(
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

    data class Ws(
        val value: String,
        override val position: Position
    ): Token {
        override fun pp(): String = value
    }

    data class Blob(
        val lexeme: String,
        override val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

    data class EOF(override val position: Position): Token {
        override fun pp() = ""
    }
}

typealias Tokens = Iterator<Token>
