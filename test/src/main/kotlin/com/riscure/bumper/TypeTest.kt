package com.riscure.bumper

import com.riscure.bumper.ast.Builtins
import com.riscure.bumper.ast.UnitDeclaration
import com.riscure.bumper.parser.UnitState
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

interface TypeTest<E,S,U: UnitState<E, S, U>>: ParseTestBase<E, S, U>  {

    companion object {
        @JvmStatic
        fun constTypes(): Stream<Arguments> = Stream.of(
            "const int i;",
            "typedef const int MyInt; MyInt i;",
            "typedef int MyInt; const MyInt i;",
            "typedef const int MyInt; typedef MyInt MyMyInt; MyMyInt i;",
            "struct A { const int x; }; struct A i;",
            "union A { char y; const int x; }; union A i;",
            "int * const i;",
            "const int i[];",
            "const int i[1][2];",
            "enum MyEnum { X }; const enum MyEnum i;",
            "const struct { int m; } A; struct A i;",
            "const union  { int m; } A; union A i;",
        ).map { Arguments.of(it) }

        @JvmStatic
        fun notConstTypes(): Stream<Arguments> = Stream.of(
            "int i;",
            "typedef int MyInt; MyInt i;",
            "typedef int MyInt; typedef MyInt MyMyInt; MyMyInt i;",
            "struct A { int x; }; struct A i;",
            "union A { char y; int x; }; union A i;",
            "const int *i;"
        ).map { Arguments.of(it) }

        @JvmStatic
        fun completeTypes(): Stream<Arguments> = Stream.of(
            "int i;",
            "struct A { } i;",
            "struct A { int m; } i;",
            "struct A { struct A *a; } i;",
            "struct A { struct B { struct A* a; } b; } i;",
            "typedef int (F)(); F i;",
            "struct A { int m; int xs[]; } i;", // variable-length array member
        ).map { Arguments.of(it) }

        @JvmStatic
        fun incompleteTypes(): Stream<Arguments> = Stream.of(
            "struct A i;",
        ).map { Arguments.of(it) }
    }

    @ParameterizedTest(name = "type of if in {0} is constant")
    @MethodSource("constTypes")
    fun testConstant(program: String) = roundtrip(program) { ast ->
        val i = assertIs<UnitDeclaration.Var<*>>(ast.variables.find { it.ident == "i" })
        assertTrue(i.type.isConstant(ast.typeEnv(Builtins.clang)))
    }

    @ParameterizedTest(name = "type of if in {0} is not constant")
    @MethodSource("notConstTypes")
    fun testNotConstant(program: String) = roundtrip(program) { ast ->
        val i = assertIs<UnitDeclaration.Var<*>>(ast.variables.find { it.ident == "i" })
        assertFalse(i.type.isConstant(ast.typeEnv(Builtins.clang)))
    }

    @ParameterizedTest(name = "type of if in {0} is complete")
    @MethodSource("completeTypes")
    fun testComplete(program: String) = roundtrip(program) { ast ->
        val i = assertIs<UnitDeclaration.Valuelike<*,*>>(ast.declarations.find { it.ident == "i" })
        assertTrue(i.type.isComplete(ast.typeEnv(Builtins.clang)))
    }

    @ParameterizedTest(name = "type of if in {0} is incomplete")
    @MethodSource("incompleteTypes")
    fun testIncomplete(program: String) = roundtrip(program) { ast ->
        val i = assertIs<UnitDeclaration.Valuelike<*,*>>(ast.declarations.find { it.ident == "i" })
        assertFalse(i.type.isComplete(ast.typeEnv(Builtins.clang)))
    }
}