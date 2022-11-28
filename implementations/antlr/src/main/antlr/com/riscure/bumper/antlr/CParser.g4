parser grammar CParser;

@header {
package com.riscure.bumper.antlr;
}

options {
  tokenVocab=CLexer;
}

program : struct_or_union_specifier EOF;

// This grammar follows the spec of the CompCert verified C parser.
// https://github.com/AbsInt/CompCert/blob/master/cparser/Parser.vy

identifier_list:
    VAR_NAME
  | identifier_list COMMA VAR_NAME
  ;

// 6.7.1
storage_class_specifier :
    TYPEDEF
  | EXTERN
  | STATIC
  | AUTO
  | REGISTER;

// 6.7.2
type_specifier:
    VOID                                     # void
  | CHAR                                     # char
  | SHORT                                    # short
  | INT                                      # int
  | LONG                                     # long
  | FLOAT                                    # float
  | DOUBLE                                   # double
  | SIGNED                                   # signed
  | UNSIGNED                                 # unsigned
  | BOOL                                     # bool
  | struct_or_union_specifier                # composite
  // | enum_specifier                           # enum TODO
  | TYPEDEF_NAME                             # named
  ;

// 6.7.2.1
struct_or_union_specifier :
    struct_or_union attribute_specifier_list ID LBRACE struct_declaration_list RBRACE
  | struct_or_union attribute_specifier_list LBRACE struct_declaration_list RBRACE
  | struct_or_union attribute_specifier_list ID
  ;

struct_or_union :
    STRUCT
  | UNION
  ;

struct_declaration_list:
    /* empty */
  | struct_declaration_list struct_declaration
  ;

struct_declaration:
    specifier_qualifier_list struct_declarator_list SEMICOLON
  | specifier_qualifier_list SEMICOLON
  ; // TODO missing one case for C11 static assertions

specifier_qualifier_list:
    type_specifier specifier_qualifier_list
  | type_specifier
  | type_qualifier specifier_qualifier_list
  | type_qualifier
  ;

struct_declarator_list:
    struct_declarator
  | struct_declarator_list COMMA struct_declarator
  ;

struct_declarator:
    declarator
  | declarator COLON constant_expression
  | COLON constant_expression
  ;

// 6.7.5
declarator:
    declarator_noattrend attribute_specifier_list
  ;

declarator_noattrend:
    direct_declarator
  | pointer direct_declarator
  ;

direct_declarator:
    ID
  | LPAREN declarator RPAREN
  | direct_declarator LBRACK type_qualifier_list assignment_expression RBRACK
  | direct_declarator LBRACK assignment_expression RBRACK
  | direct_declarator LBRACK type_qualifier_list RBRACK
  | direct_declarator LBRACK RBRACK
  | direct_declarator LPAREN parameter_type_list RPAREN
  | direct_declarator LPAREN RPAREN
  | direct_declarator LPAREN identifier_list RPAREN
  ;

pointer:
    STAR
  | STAR type_qualifier_list
  | STAR pointer
  | STAR type_qualifier_list pointer
  ;

type_qualifier_list:
    type_qualifier
  | type_qualifier_list type_qualifier
  ;

type_qualifier:
    type_qualifier_noattr
  | attribute_specifier
  ;

attribute_specifier:
    ATTRIBUTE LPAREN LPAREN gcc_attribute_list RPAREN RPAREN
  | PACKED LPAREN argument_expression_list RPAREN
  | ALIGNAS LPAREN argument_expression_list RPAREN
  | ALIGNAS LPAREN type_name RPAREN
  ;

attribute_specifier_list:
    /* empty */
  | attribute_specifier attribute_specifier_list
  ;

gcc_attribute_list:
    gcc_attribute
  | gcc_attribute_list COMMA gcc_attribute
  ;

gcc_attribute:
    /* empty */
  | gcc_attribute_word
  | gcc_attribute_word LPAREN RPAREN
  | gcc_attribute_word LPAREN argument_expression_list RPAREN
  ;

gcc_attribute_word:
    OTHER_NAME
  | CONST
  | PACKED
  ;

type_qualifier_noattr:
    CONST
  | RESTRICT
  | VOLATILE
  ;

parameter_type_list:
    parameter_list
  | parameter_list COMMA ELLIPSIS
  ;

parameter_list:
    parameter_declaration
  | parameter_list COMMA parameter_declaration
  ;

parameter_declaration:
    declaration_specifiers declarator
  | declaration_specifiers abstract_declarator
  | declaration_specifiers
  ;

abstract_declarator:
    pointer
  | pointer direct_abstract_declarator
  | direct_abstract_declarator
  ;

direct_abstract_declarator:
    LPAREN abstract_declarator RPAREN
  | direct_abstract_declarator LBRACK type_qualifier_list assignment_expression RBRACK
  | LBRACK type_qualifier_list assignment_expression RBRACK
  | direct_abstract_declarator LBRACK assignment_expression RBRACK
  | LBRACK assignment_expression RBRACK
  | direct_abstract_declarator LBRACK type_qualifier_list RBRACK
  | LBRACK type_qualifier_list RBRACK
  | direct_abstract_declarator LBRACK RBRACK
  | LBRACK RBRACK
  | direct_abstract_declarator LPAREN RPAREN
  | LPAREN RPAREN
  ;

argument_expression_list:
    assignment_expression
  | argument_expression_list COMMA assignment_expression
  ;

declaration_specifiers:
    storage_class_specifier declaration_specifiers
  | type_specifier declaration_specifiers_typespec_opt
  | type_qualifier_noattr declaration_specifiers
  | attribute_specifier declaration_specifiers
  | function_specifier declaration_specifiers
  ;

declaration_specifiers_typespec_opt:
    storage_class_specifier declaration_specifiers_typespec_opt
  | type_specifier declaration_specifiers_typespec_opt
  | type_qualifier declaration_specifiers_typespec_opt
  | function_specifier declaration_specifiers_typespec_opt
  | /* empty */
  ;

function_specifier:
    INLINE
  | NORETURN
  ;

type_name:
    specifier_qualifier_list
  | specifier_qualifier_list abstract_declarator
  ;

// 6.7.2.2
// enum_specifier

constant_expression:
    conditional_expression
  ;

// 6.5.1
primary_expression:
    var = VAR_NAME
  | cst = CONSTANT
  // | str = STRING_LITERAL // TODO
  | loc = LPAREN expr = expression RPAREN
  | sel = generic_selection
  ;

// 6.5.1.1
generic_selection:
    loc = GENERIC LPAREN expr = assignment_expression COMMA
                         alist = generic_assoc_list RPAREN
  ;
generic_assoc_list:
    a = generic_association
  | l = generic_assoc_list COMMA a = generic_association
  ;

generic_association:
    tname = type_name COLON expr = assignment_expression
  | DEFAULT COLON expr = assignment_expression
  ;

// 6.5.2
postfix_expression:
    primexpr = primary_expression
  | expr = postfix_expression LBRACK index = expression RBRACK
  | expr = postfix_expression LPAREN args = argument_expression_list RPAREN
  | expr = postfix_expression LPAREN RPAREN
  | loc = BUILTIN_VA_ARG LPAREN asgnexpr = assignment_expression COMMA ty = type_name RPAREN
  | expr = postfix_expression DOT mem = OTHER_NAME
  | expr = postfix_expression PTR mem = OTHER_NAME
  | expr = postfix_expression INC
  | expr = postfix_expression DEC
  | loc = LPAREN typ = type_name RPAREN LBRACE init = initializer_list RBRACE
  | loc = LPAREN typ = type_name RPAREN LBRACE init = initializer_list COMMA RBRACE
  | loc = BUILTIN_OFFSETOF LPAREN typ = type_name COMMA id = OTHER_NAME
        mems = designator_list RPAREN
  | loc = BUILTIN_OFFSETOF LPAREN typ = type_name COMMA mem = OTHER_NAME RPAREN
  ;

// 6.5.3
unary_expression:
    expr = postfix_expression
  | INC unexpr = unary_expression
  | DEC unexpr = unary_expression
  | op = unary_operator castexpr = cast_expression
  | SIZEOF unexpr = unary_expression
  | SIZEOF LPAREN typ = type_name RPAREN
  | ALIGNOF LPAREN typ = type_name RPAREN
  ;

unary_operator:
    AND
  | STAR
  | PLUS
  | MINUS
  | TILDE
  | BANG
  ;

cast_expression:
    unexpr = unary_expression
  | loc = LPAREN typ = type_name RPAREN castexpr = cast_expression
  ;

multiplicative_expression:
    expr = cast_expression
  | expr1 = multiplicative_expression STAR expr2 = cast_expression
  | expr1 = multiplicative_expression SLASH expr2 = cast_expression
  | expr1 = multiplicative_expression PERCENT expr2 = cast_expression
  ;

additive_expression:
    expr = multiplicative_expression
  | expr1 = additive_expression PLUS expr2 = multiplicative_expression
  | expr1 = additive_expression MINUS expr2 = multiplicative_expression
  ;

shift_expression:
  expr = additive_expression
  | expr1 = shift_expression LEFT expr2 = additive_expression
  | expr1 = shift_expression RIGHT expr2 = additive_expression
  ;

relational_expression:
    expr = shift_expression
  | expr1 = relational_expression LT expr2 = shift_expression
  | expr1 = relational_expression GT expr2 = shift_expression
  | expr1 = relational_expression LEQ expr2 = shift_expression
  | expr1 = relational_expression GEQ expr2 = shift_expression
  ;

equality_expression:
    expr = relational_expression
  | expr1 = equality_expression EQEQ expr2 = relational_expression
  | expr1 = equality_expression NEQ expr2 = relational_expression
  ;

and_expression:
    expr = equality_expression
  | expr1 = and_expression AND expr2 = equality_expression
  ;

exclusive_OR_expression:
    expr = and_expression
  | expr1 = exclusive_OR_expression HAT expr2 = and_expression
  ;

inclusive_OR_expression:
    expr = exclusive_OR_expression
  | expr1 = inclusive_OR_expression BAR expr2 = exclusive_OR_expression
  ;

logical_and_expression:
    expr = inclusive_OR_expression
  | expr1 = logical_and_expression ANDAND expr2 = inclusive_OR_expression
  ;

logical_OR_expression:
    expr = logical_and_expression
  | expr1 = logical_OR_expression BARBAR expr2 = logical_and_expression
  ;

conditional_expression:
    expr = logical_OR_expression
  | expr1 = logical_OR_expression QUESTION expr2 = expression COLON
      expr3 = conditional_expression
  ;

assignment_expression:
    expr = conditional_expression
  | expr1 = unary_expression op = assignment_operator expr2 = assignment_expression
  ;

assignment_operator:
    EQ
  | MUL_ASSIGN
  | DIV_ASSIGN
  | MOD_ASSIGN
  | ADD_ASSIGN
  | SUB_ASSIGN
  | LEFT_ASSIGN
  | RIGHT_ASSIGN
  | XOR_ASSIGN
  | OR_ASSIGN
  | AND_ASSIGN
  ;

expression:
    expr = assignment_expression
  | expr1 = expression COMMA expr2 = assignment_expression
  ;


// 6.7.8
c_initializer:
    expr = assignment_expression
  | LBRACE init = initializer_list RBRACE
  | LBRACE init = initializer_list COMMA RBRACE
  ;

initializer_list:
    design = designation init = c_initializer
  | init = c_initializer
  | initq = initializer_list COMMA design = designation init = c_initializer
  | initq = initializer_list COMMA init = c_initializer
  ;

designation:
    design = designator_list EQ
  ;

designator_list:
    design = designator
  | designq = designator_list designt = designator
  ;

designator:
    LBRACK expr = constant_expression RBRACK
  | DOT id = OTHER_NAME
  ;