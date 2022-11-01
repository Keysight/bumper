package com.riscure.langs.c.transform


/**
 * Transform a C AST, hoisting all globally visible declarations to the top.
 * E.g.,
 *
 * struct A { struct B { int i; } b; };
 *
 * will, after hoisting, look like:
 *
 * struct B { int i; };
 * struct A { struct B b; };
 */