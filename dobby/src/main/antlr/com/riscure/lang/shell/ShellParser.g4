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

arg : val+;

val : D_QUOTEDSTRING
    | S_QUOTEDSTRING
    | rawstring;

rawstring : CHAR+;
