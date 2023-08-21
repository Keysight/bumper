package com.riscure.bumper.highlight.lexer;

import com.riscure.bumper.highlight.LexerException;
import com.riscure.bumper.highlight.Token;
import com.riscure.bumper.highlight.Position;
import com.riscure.bumper.highlight.Keywords;
import com.riscure.bumper.highlight.StrEncoding;
import java.util.Arrays;

%%

%class CLexer
%unicode
%line
%column
%type Token
%public
%yylexthrow LexerException

%{
  String encoding = "";
  StringBuffer string = new StringBuffer();

  public Position getPos() {
    return new Position(yyline + 1, yycolumn + 1);
  }

  private Token keyword(Keywords k) {
    return new Token.Keyword(k, yytext(), getPos());
  }

  private Token punct() {
    return new Token.Punctuation(yytext(), getPos());
  }

  private void charBegin(String openLexeme) {
    string.setLength(0);
    encoding = openLexeme.substring(0, openLexeme.length() - 1);
    yybegin(CHAR);
  }

  private void stringBegin(String openLexeme) {
    string.setLength(0);
    encoding = openLexeme.substring(0, openLexeme.length() - 1);
    yybegin(STRING);
  }

  private Token charEnd() {
    yybegin(YYINITIAL);
    return new Token.CharLiteral(StrEncoding.parse(encoding), string.toString(), getPos());
  }

  private Token stringEnd() {
    yybegin(YYINITIAL);
    return new Token.StringLiteral(StrEncoding.parse(encoding), string.toString(), getPos());
  }

  private void multilineCommentBegin() {
    string.setLength(0);
    yybegin(MULTILINE_COMMENT);
  }

  private Token multilineCommentEnd() {
    yybegin(YYINITIAL);
    return new Token.MultiLineComment(string.toString(), getPos());
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
Whitespace     = ({LineTerminator} | [ \t\f])+
BlobCharacter  = [a-zA-Z0-9_$]
Identifier     = ([:jletter:] | [$]) [:jletterdigit:]*
Directive      = [a-zA-Z]+

MLCOMMENTCHAR  = !"*/"

%state STRING
%state CHAR
%state COMMENT
%state MULTILINE_COMMENT

%%

<YYINITIAL> {
   {Whitespace}                                { return new Token.Ws(yytext(), getPos()); }
    <<EOF>>                                    { return new Token.EOF(getPos()); }

    "#" {Directive} .*                          { return new Token.Directive(yytext(), getPos()); }

    (""|"L"|"u"|"U") "'"                       { charBegin(yytext()); }
    (""|"L"|"u"|"U"|"u8"|"R") \"               { stringBegin(yytext()); }
    "/*"                                       { multilineCommentBegin(); }

    "void" | "char" | "short" | "int" | "long" { return new Token.Type(yytext(), getPos()); }
    "float" | "double"                         { return new Token.Type(yytext(), getPos()); }

    "_Alignas"                                 { return keyword(Keywords.AlignAs); }
    "_Alignof"                                 { return keyword(Keywords.AlignOf); }
    "_Bool"                                    { return keyword(Keywords.Bool); }
    "_Generic"                                 { return keyword(Keywords.Generic); }
    "_Complex"                                 { return keyword(Keywords.Complex); }
    "_Imaginary"                               { return keyword(Keywords.Imaginary); }
    "_Static_assert"                           { return keyword(Keywords.StaticAssert); }
    "__alignof"                                { return keyword(Keywords.AlignOf); }
    "__alignof__"                              { return keyword(Keywords.AlignOf); }
    "__asm"                                    { return keyword(Keywords.Asm); }
    "__asm__"                                  { return keyword(Keywords.Asm); }
    "__attribute"                              { return keyword(Keywords.Attribute); }
    "__attribute__"                            { return keyword(Keywords.Attribute); }
    "__builtin_va_arg"                         { return keyword(Keywords.VaArg); }
    "__builtin_offsetof"                       { return keyword(Keywords.OffsetOf); }
    "__const"                                  { return keyword(Keywords.Const); }
    "__const__"                                { return keyword(Keywords.Const); }
    "__inline"                                 { return keyword(Keywords.Inline); }
    "__inline__"                               { return keyword(Keywords.Inline); }
    "__packed__"                               { return keyword(Keywords.Packed); }
    "__restrict"                               { return keyword(Keywords.Restrict); }
    "__restrict__"                             { return keyword(Keywords.Restrict); }
    "__signed"                                 { return keyword(Keywords.Signed); }
    "__signed__"                               { return keyword(Keywords.Signed); }
    "__volatile"                               { return keyword(Keywords.Volatile); }
    "__volatile__"                             { return keyword(Keywords.Volatile); }
    "asm"                                      { return keyword(Keywords.Asm); }
    "auto"                                     { return keyword(Keywords.Auto); }
    "break"                                    { return keyword(Keywords.Break); }
    "case"                                     { return keyword(Keywords.Case); }
    "const"                                    { return keyword(Keywords.Const); }
    "continue"                                 { return keyword(Keywords.Continue); }
    "default"                                  { return keyword(Keywords.Default); }
    "do"                                       { return keyword(Keywords.Do); }
    "else"                                     { return keyword(Keywords.Else); }
    "enum"                                     { return keyword(Keywords.Enum); }
    "extern"                                   { return keyword(Keywords.Extern); }
    "for"                                      { return keyword(Keywords.For); }
    "goto"                                     { return keyword(Keywords.Goto); }
    "if"                                       { return keyword(Keywords.If); }
    "inline"                                   { return keyword(Keywords.Inline); }
    "_Noreturn"                                { return keyword(Keywords.NoReturn); }
    "register"                                 { return keyword(Keywords.Register); }
    "restrict"                                 { return keyword(Keywords.Restrict); }
    "return"                                   { return keyword(Keywords.Return); }
    "signed"                                   { return keyword(Keywords.Signed); }
    "sizeof"                                   { return keyword(Keywords.Sizeof); }
    "static"                                   { return keyword(Keywords.Static); }
    "struct"                                   { return keyword(Keywords.Struct); }
    "switch"                                   { return keyword(Keywords.Switch); }
    "typedef"                                  { return keyword(Keywords.Typedef); }
    "union"                                    { return keyword(Keywords.Union); }
    "unsigned"                                 { return keyword(Keywords.Unsigned); }
    "volatile"                                 { return keyword(Keywords.Volatile); }
    "while"                                    { return keyword(Keywords.While); }

    "..."                                      { return punct(); }
    "+="                                       { return punct(); }
    "-="                                       { return punct(); }
    "*="                                       { return punct(); }
    "/="                                       { return punct(); }
    "%="                                       { return punct(); }
    "|="                                       { return punct(); }
    "&="                                       { return punct(); }
    "^="                                       { return punct(); }
    "<<="                                      { return punct(); }
    ">>="                                      { return punct(); }
    "<<"                                       { return punct(); }
    ">>"                                       { return punct(); }
    "=="                                       { return punct(); }
    "!="                                       { return punct(); }
    "<="                                       { return punct(); }
    ">="                                       { return punct(); }
    "="                                        { return punct(); }
    "<"                                        { return punct(); }
    ">"                                        { return punct(); }
    "++"                                       { return punct(); }
    "--"                                       { return punct(); }
    "->"                                       { return punct(); }
    "+"                                        { return punct(); }
    "-"                                        { return punct(); }
    "*"                                        { return punct(); }
    "/"                                        { return punct(); }
    "%"                                        { return punct(); }
    "!"                                        { return punct(); }
    "&&"                                       { return punct(); }
    "||"                                       { return punct(); }
    "&"                                        { return punct(); }
    "|"                                        { return punct(); }
    "^"                                        { return punct(); }
    "?"                                        { return punct(); }
    ":"                                        { return punct(); }
    "~"                                        { return punct(); }
    "{"|"<%"                                   { return punct(); }
    "}"|"%>"                                   { return punct(); }
    "["|"<:"                                   { return punct(); }
    "]"|":>"                                   { return punct(); }
    "("                                        { return punct(); }
    ")"                                        { return punct(); }
    ";"                                        { return punct(); }
    ","                                        { return punct(); }
    "."                                        { return punct(); }

    {Identifier}                               { return new Token.Identifier(yytext(), getPos()); }
    {BlobCharacter}+                           { return new Token.Blob(yytext(), getPos()); }
}

<CHAR> {
    \\'                                        { string.append(yytext()); }
    '                                          { return charEnd(); }
    [^\"\n\r\\]                                { string.append(yytext()); }
}

<STRING> {
    \\\"                                       { string.append(yytext()); }
    \"                                         { return stringEnd(); }
    [^\"\n\r\\]                                { string.append(yytext()); }
}

<MULTILINE_COMMENT> {
    "*/"                                       { return multilineCommentEnd(); }
    .|\R                                       { string.append(yytext()); }
}

/* error fallback */
[^]                                            {
    throw new LexerException(getPos(), yytext());
}