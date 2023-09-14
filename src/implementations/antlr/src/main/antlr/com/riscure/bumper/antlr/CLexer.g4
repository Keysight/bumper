lexer grammar CLexer;

@header {
package com.riscure.bumper.antlr;
}

WS       : [ \t\r\n]+ -> skip;
INTLIT   : DIGIT+;

fragment DIGIT : [0-9];
fragment ALPHA : [a-zA-Z];
fragment UNDERSCORE : '_';

// keywords https://learn.microsoft.com/en-us/cpp/c-language/c-keywords
// some still missing

CONST  : 'const';
PACKED : 'packed';
TYPEDEF: 'typedef';
AUTO   : 'auto';
EXTERN : 'extern';
INLINE : 'inline';
NORETURN : 'noreturn';
STATIC : 'static';
REGISTER : 'register';
VOLATILE : 'volatile';
RESTRICT : 'restrict';
UNSIGNED : 'unsigned';
SIGNED   : 'signed';
SIZEOF : 'sizeof';
ALIGNAS : 'alignas' | '_Alignas';
ALIGNOF : '_Alignof' | '__alignof__' | '__alignof';

FLOAT  : 'float';
DOUBLE : 'double';

VOID   : 'void';
CHAR   : 'char';
SHORT  : 'short';
INT    : 'int';
LONG   : 'long';

STRUCT : 'struct';
UNION  : 'union';
ENUM   : 'enum';

ATOMIC  : '_Atomic';
BOOL    : '_Bool';
COMPLEX : '_Complex';
BUILTIN_VA_ARG : '__builtin_va_arg';
BUILTIN_OFFSETOF  : '__builtin_offsetof';

ATTRIBUTE : '__attribute' | '__attribute__';
GENERIC   : '_Generic';
DEFAULT   : 'default';

ELLIPSIS : '...';
ADD_ASSIGN : '+=';
SUB_ASSIGN : '-=';
MUL_ASSIGN : '*=';
DIV_ASSIGN : '/=';
MOD_ASSIGN : '%=';
OR_ASSIGN : '|=';
AND_ASSIGN : '&=';
XOR_ASSIGN : '^=';
LEFT_ASSIGN : '<<=';
RIGHT_ASSIGN : '>>=';
LEFT : '<<';
RIGHT : '>>';
EQEQ : '==';
NEQ : '!=';
LEQ : '<=';
GEQ : '>=';
EQ : '=';
LT : '<';
GT : '>';
INC : '++';
DEC : '--';
PTR : '->';
PLUS : '+';
MINUS : '-';
STAR : '*';
SLASH : '/';
PERCENT : '%';
BANG : '!';
ANDAND : '&&';
BARBAR : '||';
AND : '&';
BAR : '|';
HAT : '^';
QUESTION : '?';
COLON : ':';
TILDE : '~';
LBRACE : '{'|'<%';
RBRACE : '}'|'%>';
LBRACK : '['|'<:';
RBRACK : ']'|':>';
LPAREN : '(';
RPAREN : ')';
COMMA : ',';
DOT : '.';
SEMICOLON : ';';

CONSTANT : [0-9]+; // TODO

ID           : (UNDERSCORE | ALPHA) (UNDERSCORE | DIGIT | ALPHA)*;
OTHER_NAME   : (UNDERSCORE | ALPHA) (UNDERSCORE | DIGIT | ALPHA)*; // TODO?
VAR_NAME     : (UNDERSCORE | ALPHA) (UNDERSCORE | DIGIT | ALPHA)*; // TODO?
TYPEDEF_NAME : (UNDERSCORE | ALPHA) (UNDERSCORE | DIGIT | ALPHA)*; // TODO?
