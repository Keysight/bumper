/**
 * This implements a C lexer with support for coverage highlighting.
 */
package com.riscure.bumper.highlight

import java.nio.file.Path

data class Coverage(
    val count: Int
)

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

    data class Identifier(
        val ident: String,
        val position: Position
    ): Token {
        override fun pp(): String = ident
    }

    data class Type(
        val lexeme: String,
        val position: Position,
    ): Token {
        override fun pp(): String = lexeme
    }

    data class Keyword(
        val word: Keywords,
        val lexeme: String,
        val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

//    data class Operator(
//        val op: Op,
//        val position: Position,
//    ): Token

    data class Punctuation(
        val lexeme: String,
        val position: Position,
    ): Token {
        override fun pp(): String = lexeme
    }

    data class CharLiteral(
        val encoding: StrEncoding,
        val lexeme: String,
        val position: Position
    ): Token {
        override fun pp(): String = "${encoding.pp()}'$lexeme'"
    }

    data class StringLiteral(
        val encoding: StrEncoding,
        val lexeme: String,
        val position: Position
    ): Token {
        override fun pp(): String = "${encoding.pp()}\"$lexeme\""
    }

    data class IntLiteral(
        val lexeme: String,
        val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

    data class Ws(
        val value: String,
        val position: Position
    ): Token {
        override fun pp(): String = value
    }

    data class Blob(
        val lexeme: String,
        val position: Position
    ): Token {
        override fun pp(): String = lexeme
    }

    object EOF: Token {
        override fun pp() = ""
    }

    companion object {
        @JvmField
        val eof = EOF
    }
}

data class LineSegment(
    val tokens: List<Token>,
    val coverage: Coverage,
)

data class Line(
    val segments: LineSegment
)

data class Source(
    /**
     * Some path (absolute/relative/symbolic) from where this source originates.
     */
    val path: Path,

    /**
     * The line-number of the first line in [lines] in [path].
     */
    val lineStart: Int,

    /**
     * The lines in this source blob.
     */
    val lines: List<Line>
)
