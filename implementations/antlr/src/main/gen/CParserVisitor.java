// Generated from /home/arjen/projects/tc.java/lib/bumper/implementations/antlr/src/main/antlr/com/riscure/bumper/antlr/CParser.g4 by ANTLR 4.10.1

package com.riscure.bumper.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(CParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#identifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier_list(CParser.Identifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#storage_class_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_class_specifier(CParser.Storage_class_specifierContext ctx);
	/**
	 * Visit a parse tree produced by the {@code void}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVoid(CParser.VoidContext ctx);
	/**
	 * Visit a parse tree produced by the {@code char}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChar(CParser.CharContext ctx);
	/**
	 * Visit a parse tree produced by the {@code short}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShort(CParser.ShortContext ctx);
	/**
	 * Visit a parse tree produced by the {@code int}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt(CParser.IntContext ctx);
	/**
	 * Visit a parse tree produced by the {@code long}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLong(CParser.LongContext ctx);
	/**
	 * Visit a parse tree produced by the {@code float}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloat(CParser.FloatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code double}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDouble(CParser.DoubleContext ctx);
	/**
	 * Visit a parse tree produced by the {@code signed}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSigned(CParser.SignedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code unsigned}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnsigned(CParser.UnsignedContext ctx);
	/**
	 * Visit a parse tree produced by the {@code bool}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBool(CParser.BoolContext ctx);
	/**
	 * Visit a parse tree produced by the {@code composite}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite(CParser.CompositeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code named}
	 * labeled alternative in {@link CParser#type_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamed(CParser.NamedContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_or_union_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_or_union_specifier(CParser.Struct_or_union_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_or_union}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_or_union(CParser.Struct_or_unionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_declaration_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declaration_list(CParser.Struct_declaration_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declaration(CParser.Struct_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#specifier_qualifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSpecifier_qualifier_list(CParser.Specifier_qualifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_declarator_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declarator_list(CParser.Struct_declarator_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#struct_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStruct_declarator(CParser.Struct_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarator(CParser.DeclaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#declarator_noattrend}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclarator_noattrend(CParser.Declarator_noattrendContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#direct_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirect_declarator(CParser.Direct_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#pointer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPointer(CParser.PointerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#type_qualifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_qualifier_list(CParser.Type_qualifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#type_qualifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_qualifier(CParser.Type_qualifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#attribute_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_specifier(CParser.Attribute_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#attribute_specifier_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_specifier_list(CParser.Attribute_specifier_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#gcc_attribute_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGcc_attribute_list(CParser.Gcc_attribute_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#gcc_attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGcc_attribute(CParser.Gcc_attributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#gcc_attribute_word}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGcc_attribute_word(CParser.Gcc_attribute_wordContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#type_qualifier_noattr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_qualifier_noattr(CParser.Type_qualifier_noattrContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#parameter_type_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_type_list(CParser.Parameter_type_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#parameter_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_list(CParser.Parameter_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#parameter_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_declaration(CParser.Parameter_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#abstract_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAbstract_declarator(CParser.Abstract_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#direct_abstract_declarator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirect_abstract_declarator(CParser.Direct_abstract_declaratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#argument_expression_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument_expression_list(CParser.Argument_expression_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#declaration_specifiers}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration_specifiers(CParser.Declaration_specifiersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#declaration_specifiers_typespec_opt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclaration_specifiers_typespec_opt(CParser.Declaration_specifiers_typespec_optContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#function_specifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_specifier(CParser.Function_specifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name(CParser.Type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#constant_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant_expression(CParser.Constant_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#primary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_expression(CParser.Primary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#generic_selection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_selection(CParser.Generic_selectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#generic_assoc_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_assoc_list(CParser.Generic_assoc_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#generic_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneric_association(CParser.Generic_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#postfix_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPostfix_expression(CParser.Postfix_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(CParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#unary_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_operator(CParser.Unary_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#cast_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCast_expression(CParser.Cast_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#multiplicative_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiplicative_expression(CParser.Multiplicative_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#additive_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdditive_expression(CParser.Additive_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#shift_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShift_expression(CParser.Shift_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#relational_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_expression(CParser.Relational_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#equality_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEquality_expression(CParser.Equality_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnd_expression(CParser.And_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#exclusive_OR_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExclusive_OR_expression(CParser.Exclusive_OR_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#inclusive_OR_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInclusive_OR_expression(CParser.Inclusive_OR_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#logical_and_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_and_expression(CParser.Logical_and_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#logical_OR_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_OR_expression(CParser.Logical_OR_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#conditional_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_expression(CParser.Conditional_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#assignment_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_expression(CParser.Assignment_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#assignment_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_operator(CParser.Assignment_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(CParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#c_initializer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_initializer(CParser.C_initializerContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#initializer_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializer_list(CParser.Initializer_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#designation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignation(CParser.DesignationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#designator_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignator_list(CParser.Designator_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link CParser#designator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesignator(CParser.DesignatorContext ctx);
}