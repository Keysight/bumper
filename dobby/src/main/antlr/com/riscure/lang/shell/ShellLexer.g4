lexer grammar ShellLexer;

@header {
package com.riscure.lang.shell;
}


R_ESC_WS       : '\\' [ ] -> type(CHAR);
WS             : [ \t]+;
BR             : [\n\r]+;

// High precedenc chars
R_ESC_ESC      : '\\\\'    -> type(CHAR);
R_ESC_QUOTE    : '\\' ['"] -> type(CHAR);

// A (double) quote starts string mode
OPEN_DOUBLE    : ["] -> pushMode(D_STRINGS);
OPEN_SINGLE    : ['] -> pushMode(S_STRINGS);

// Low precedence chars
CHAR : .;

mode D_STRINGS ;
// escaped escaped characters have higher precedence than escaped quotes
D_ESC_ESC      : '\\\\' -> type(CHAR);
// escaped quotes have higher precedence than closing quotes
D_ESC_QUOTE    : '\\"' -> type(CHAR);
// the closing quotes eats all the built-up input
// and we end the string mode
CLOSE_DOUBLE   : ["]  -> popMode;
// Anything else in string mode we eat.
D_CHAR         : . -> type(CHAR);

// similar, but for single quotes
mode S_STRINGS ;
S_ESC_ESC      : '\\\\'-> type(CHAR);
S_ESC_QUOTE    : '\\\'' -> type(CHAR);
CLOSE_SINGLE   : [']  -> popMode;
S_CHAR         : . -> type(CHAR);

