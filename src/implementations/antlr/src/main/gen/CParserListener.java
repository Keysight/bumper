// Generated from /home/arjen/projects/tc.java/lib/bumper/implementations/antlr/src/main/antlr/com/riscure/bumper/antlr/CParser.g4 by ANTLR 4.10.1

package com.riscure.bumper.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link CParser}.
 */
public interface CParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link CParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(CParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(CParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#identifier_list}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier_list(CParser.Identifier_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#identifier_list}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier_list(CParser.Identifier_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#storage_class_specifier}.
	 * @param ctx the parse tree
	 */
	void enterStorage_class_specifier(CParser.Storage_class_specifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#storage_class_specifier}.
	 * @param ctx the parse tree
	 */
	void exitStorage_class_specifier(CParser.Storage_class_specifierContext ctx);
	/**
	 * Enter a parse tree produced by the {@code void}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterVoid(CParser.VoidContext ctx);
	/**
	 * Exit a parse tree produced by the {@code void}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitVoid(CParser.VoidContext ctx);
	/**
	 * Enter a parse tree produced by the {@code char}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterChar(CParser.CharContext ctx);
	/**
	 * Exit a parse tree produced by the {@code char}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitChar(CParser.CharContext ctx);
	/**
	 * Enter a parse tree produced by the {@code short}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterShort(CParser.ShortContext ctx);
	/**
	 * Exit a parse tree produced by the {@code short}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitShort(CParser.ShortContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterInt(CParser.IntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitInt(CParser.IntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code long}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterLong(CParser.LongContext ctx);
	/**
	 * Exit a parse tree produced by the {@code long}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitLong(CParser.LongContext ctx);
	/**
	 * Enter a parse tree produced by the {@code float}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterFloat(CParser.FloatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code float}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitFloat(CParser.FloatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code double}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterDouble(CParser.DoubleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code double}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitDouble(CParser.DoubleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code signed}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterSigned(CParser.SignedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code signed}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitSigned(CParser.SignedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unsigned}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterUnsigned(CParser.UnsignedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unsigned}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitUnsigned(CParser.UnsignedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bool}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterBool(CParser.BoolContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bool}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitBool(CParser.BoolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code composite}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterComposite(CParser.CompositeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code composite}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitComposite(CParser.CompositeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code named}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void enterNamed(CParser.NamedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code named}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 */
	void exitNamed(CParser.NamedContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_or_union_specifier}.
	 * @param ctx the parse tree
	 */
	void enterStruct_or_union_specifier(CParser.Struct_or_union_specifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_or_union_specifier}.
	 * @param ctx the parse tree
	 */
	void exitStruct_or_union_specifier(CParser.Struct_or_union_specifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_or_union}.
	 * @param ctx the parse tree
	 */
	void enterStruct_or_union(CParser.Struct_or_unionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_or_union}.
	 * @param ctx the parse tree
	 */
	void exitStruct_or_union(CParser.Struct_or_unionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_declaration_list}.
	 * @param ctx the parse tree
	 */
	void enterStruct_declaration_list(CParser.Struct_declaration_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_declaration_list}.
	 * @param ctx the parse tree
	 */
	void exitStruct_declaration_list(CParser.Struct_declaration_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_declaration}.
	 * @param ctx the parse tree
	 */
	void enterStruct_declaration(CParser.Struct_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_declaration}.
	 * @param ctx the parse tree
	 */
	void exitStruct_declaration(CParser.Struct_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#specifier_qualifier_list}.
	 * @param ctx the parse tree
	 */
	void enterSpecifier_qualifier_list(CParser.Specifier_qualifier_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#specifier_qualifier_list}.
	 * @param ctx the parse tree
	 */
	void exitSpecifier_qualifier_list(CParser.Specifier_qualifier_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_declarator_list}.
	 * @param ctx the parse tree
	 */
	void enterStruct_declarator_list(CParser.Struct_declarator_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_declarator_list}.
	 * @param ctx the parse tree
	 */
	void exitStruct_declarator_list(CParser.Struct_declarator_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#struct_declarator}.
	 * @param ctx the parse tree
	 */
	void enterStruct_declarator(CParser.Struct_declaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#struct_declarator}.
	 * @param ctx the parse tree
	 */
	void exitStruct_declarator(CParser.Struct_declaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#declarator}.
	 * @param ctx the parse tree
	 */
	void enterDeclarator(CParser.DeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#declarator}.
	 * @param ctx the parse tree
	 */
	void exitDeclarator(CParser.DeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#declarator_noattrend}.
	 * @param ctx the parse tree
	 */
	void enterDeclarator_noattrend(CParser.Declarator_noattrendContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#declarator_noattrend}.
	 * @param ctx the parse tree
	 */
	void exitDeclarator_noattrend(CParser.Declarator_noattrendContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#direct_declarator}.
	 * @param ctx the parse tree
	 */
	void enterDirect_declarator(CParser.Direct_declaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#direct_declarator}.
	 * @param ctx the parse tree
	 */
	void exitDirect_declarator(CParser.Direct_declaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#pointer}.
	 * @param ctx the parse tree
	 */
	void enterPointer(CParser.PointerContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#pointer}.
	 * @param ctx the parse tree
	 */
	void exitPointer(CParser.PointerContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#type_qualifier_list}.
	 * @param ctx the parse tree
	 */
	void enterType_qualifier_list(CParser.Type_qualifier_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#type_qualifier_list}.
	 * @param ctx the parse tree
	 */
	void exitType_qualifier_list(CParser.Type_qualifier_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#type_qualifier}.
	 * @param ctx the parse tree
	 */
	void enterType_qualifier(CParser.Type_qualifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#type_qualifier}.
	 * @param ctx the parse tree
	 */
	void exitType_qualifier(CParser.Type_qualifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#attribute_specifier}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_specifier(CParser.Attribute_specifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#attribute_specifier}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_specifier(CParser.Attribute_specifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#attribute_specifier_list}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_specifier_list(CParser.Attribute_specifier_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#attribute_specifier_list}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_specifier_list(CParser.Attribute_specifier_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#gcc_attribute_list}.
	 * @param ctx the parse tree
	 */
	void enterGcc_attribute_list(CParser.Gcc_attribute_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#gcc_attribute_list}.
	 * @param ctx the parse tree
	 */
	void exitGcc_attribute_list(CParser.Gcc_attribute_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#gcc_attribute}.
	 * @param ctx the parse tree
	 */
	void enterGcc_attribute(CParser.Gcc_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#gcc_attribute}.
	 * @param ctx the parse tree
	 */
	void exitGcc_attribute(CParser.Gcc_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#gcc_attribute_word}.
	 * @param ctx the parse tree
	 */
	void enterGcc_attribute_word(CParser.Gcc_attribute_wordContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#gcc_attribute_word}.
	 * @param ctx the parse tree
	 */
	void exitGcc_attribute_word(CParser.Gcc_attribute_wordContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#type_qualifier_noattr}.
	 * @param ctx the parse tree
	 */
	void enterType_qualifier_noattr(CParser.Type_qualifier_noattrContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#type_qualifier_noattr}.
	 * @param ctx the parse tree
	 */
	void exitType_qualifier_noattr(CParser.Type_qualifier_noattrContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#parameter_type_list}.
	 * @param ctx the parse tree
	 */
	void enterParameter_type_list(CParser.Parameter_type_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#parameter_type_list}.
	 * @param ctx the parse tree
	 */
	void exitParameter_type_list(CParser.Parameter_type_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void enterParameter_list(CParser.Parameter_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#parameter_list}.
	 * @param ctx the parse tree
	 */
	void exitParameter_list(CParser.Parameter_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#parameter_declaration}.
	 * @param ctx the parse tree
	 */
	void enterParameter_declaration(CParser.Parameter_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#parameter_declaration}.
	 * @param ctx the parse tree
	 */
	void exitParameter_declaration(CParser.Parameter_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#abstract_declarator}.
	 * @param ctx the parse tree
	 */
	void enterAbstract_declarator(CParser.Abstract_declaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#abstract_declarator}.
	 * @param ctx the parse tree
	 */
	void exitAbstract_declarator(CParser.Abstract_declaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#direct_abstract_declarator}.
	 * @param ctx the parse tree
	 */
	void enterDirect_abstract_declarator(CParser.Direct_abstract_declaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#direct_abstract_declarator}.
	 * @param ctx the parse tree
	 */
	void exitDirect_abstract_declarator(CParser.Direct_abstract_declaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#argument_expression_list}.
	 * @param ctx the parse tree
	 */
	void enterArgument_expression_list(CParser.Argument_expression_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#argument_expression_list}.
	 * @param ctx the parse tree
	 */
	void exitArgument_expression_list(CParser.Argument_expression_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#declaration_specifiers}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration_specifiers(CParser.Declaration_specifiersContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#declaration_specifiers}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration_specifiers(CParser.Declaration_specifiersContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#declaration_specifiers_typespec_opt}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration_specifiers_typespec_opt(CParser.Declaration_specifiers_typespec_optContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#declaration_specifiers_typespec_opt}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration_specifiers_typespec_opt(CParser.Declaration_specifiers_typespec_optContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#function_specifier}.
	 * @param ctx the parse tree
	 */
	void enterFunction_specifier(CParser.Function_specifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#function_specifier}.
	 * @param ctx the parse tree
	 */
	void exitFunction_specifier(CParser.Function_specifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#type_name}.
	 * @param ctx the parse tree
	 */
	void enterType_name(CParser.Type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#type_name}.
	 * @param ctx the parse tree
	 */
	void exitType_name(CParser.Type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#constant_expression}.
	 * @param ctx the parse tree
	 */
	void enterConstant_expression(CParser.Constant_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#constant_expression}.
	 * @param ctx the parse tree
	 */
	void exitConstant_expression(CParser.Constant_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#primary_expression}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_expression(CParser.Primary_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#primary_expression}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_expression(CParser.Primary_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#generic_selection}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_selection(CParser.Generic_selectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#generic_selection}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_selection(CParser.Generic_selectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#generic_assoc_list}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_assoc_list(CParser.Generic_assoc_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#generic_assoc_list}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_assoc_list(CParser.Generic_assoc_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#generic_association}.
	 * @param ctx the parse tree
	 */
	void enterGeneric_association(CParser.Generic_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#generic_association}.
	 * @param ctx the parse tree
	 */
	void exitGeneric_association(CParser.Generic_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#postfix_expression}.
	 * @param ctx the parse tree
	 */
	void enterPostfix_expression(CParser.Postfix_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#postfix_expression}.
	 * @param ctx the parse tree
	 */
	void exitPostfix_expression(CParser.Postfix_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expression(CParser.Unary_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expression(CParser.Unary_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#unary_operator}.
	 * @param ctx the parse tree
	 */
	void enterUnary_operator(CParser.Unary_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#unary_operator}.
	 * @param ctx the parse tree
	 */
	void exitUnary_operator(CParser.Unary_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#cast_expression}.
	 * @param ctx the parse tree
	 */
	void enterCast_expression(CParser.Cast_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#cast_expression}.
	 * @param ctx the parse tree
	 */
	void exitCast_expression(CParser.Cast_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#multiplicative_expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicative_expression(CParser.Multiplicative_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#multiplicative_expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicative_expression(CParser.Multiplicative_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#additive_expression}.
	 * @param ctx the parse tree
	 */
	void enterAdditive_expression(CParser.Additive_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#additive_expression}.
	 * @param ctx the parse tree
	 */
	void exitAdditive_expression(CParser.Additive_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#shift_expression}.
	 * @param ctx the parse tree
	 */
	void enterShift_expression(CParser.Shift_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#shift_expression}.
	 * @param ctx the parse tree
	 */
	void exitShift_expression(CParser.Shift_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void enterRelational_expression(CParser.Relational_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void exitRelational_expression(CParser.Relational_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#equality_expression}.
	 * @param ctx the parse tree
	 */
	void enterEquality_expression(CParser.Equality_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#equality_expression}.
	 * @param ctx the parse tree
	 */
	void exitEquality_expression(CParser.Equality_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#and_expression}.
	 * @param ctx the parse tree
	 */
	void enterAnd_expression(CParser.And_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#and_expression}.
	 * @param ctx the parse tree
	 */
	void exitAnd_expression(CParser.And_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#exclusive_OR_expression}.
	 * @param ctx the parse tree
	 */
	void enterExclusive_OR_expression(CParser.Exclusive_OR_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#exclusive_OR_expression}.
	 * @param ctx the parse tree
	 */
	void exitExclusive_OR_expression(CParser.Exclusive_OR_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#inclusive_OR_expression}.
	 * @param ctx the parse tree
	 */
	void enterInclusive_OR_expression(CParser.Inclusive_OR_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#inclusive_OR_expression}.
	 * @param ctx the parse tree
	 */
	void exitInclusive_OR_expression(CParser.Inclusive_OR_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#logical_and_expression}.
	 * @param ctx the parse tree
	 */
	void enterLogical_and_expression(CParser.Logical_and_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#logical_and_expression}.
	 * @param ctx the parse tree
	 */
	void exitLogical_and_expression(CParser.Logical_and_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#logical_OR_expression}.
	 * @param ctx the parse tree
	 */
	void enterLogical_OR_expression(CParser.Logical_OR_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#logical_OR_expression}.
	 * @param ctx the parse tree
	 */
	void exitLogical_OR_expression(CParser.Logical_OR_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#conditional_expression}.
	 * @param ctx the parse tree
	 */
	void enterConditional_expression(CParser.Conditional_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#conditional_expression}.
	 * @param ctx the parse tree
	 */
	void exitConditional_expression(CParser.Conditional_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#assignment_expression}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_expression(CParser.Assignment_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#assignment_expression}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_expression(CParser.Assignment_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#assignment_operator}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_operator(CParser.Assignment_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#assignment_operator}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_operator(CParser.Assignment_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(CParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(CParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#c_initializer}.
	 * @param ctx the parse tree
	 */
	void enterC_initializer(CParser.C_initializerContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#c_initializer}.
	 * @param ctx the parse tree
	 */
	void exitC_initializer(CParser.C_initializerContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#initializer_list}.
	 * @param ctx the parse tree
	 */
	void enterInitializer_list(CParser.Initializer_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#initializer_list}.
	 * @param ctx the parse tree
	 */
	void exitInitializer_list(CParser.Initializer_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#designation}.
	 * @param ctx the parse tree
	 */
	void enterDesignation(CParser.DesignationContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#designation}.
	 * @param ctx the parse tree
	 */
	void exitDesignation(CParser.DesignationContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#designator_list}.
	 * @param ctx the parse tree
	 */
	void enterDesignator_list(CParser.Designator_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#designator_list}.
	 * @param ctx the parse tree
	 */
	void exitDesignator_list(CParser.Designator_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link CParser#designator}.
	 * @param ctx the parse tree
	 */
	void enterDesignator(CParser.DesignatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link CParser#designator}.
	 * @param ctx the parse tree
	 */
	void exitDesignator(CParser.DesignatorContext ctx);
}