package com.riscure.bumper.highlight.lexer;

import com.riscure.bumper.highlight.Token;
import com.riscure.bumper.highlight.Position;

%%

%class CLexer
%unicode
%line
%column
%type Token
%public

%{
  StringBuffer string = new StringBuffer();

  private Position getPos() {
    return new Position(yyline, yycolumn);
  }
%}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
Whitespace     = {LineTerminator} | [ \t\f]

Identifier = [:jletter:] [:jletterdigit:]*

%state STRING

%%

<YYINITIAL> {
    "hello"                 { return new Token.StringLiteral("hello", getPos()); }
    {Whitespace}            { /* ignore */ }

}