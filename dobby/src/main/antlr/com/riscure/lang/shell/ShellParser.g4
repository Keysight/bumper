parser grammar ShellParser;

@header {
package com.riscure.lang.shell;
}

options {
  tokenVocab=ShellLexer;
}

line : WS? args=shellargs WS? EOF;

shellargs : head=arg WS tail=shellargs # cons
          | single=arg                 # single
          |                            # nil
          ;

arg : parts=val+;

val : OPEN_DOUBLE value=CHAR* CLOSE_DOUBLE   # doubleQuoted
    | OPEN_SINGLE value=CHAR* CLOSE_SINGLE   # singleQuoted
    | value=CHAR+                            # unquoted
    ;
