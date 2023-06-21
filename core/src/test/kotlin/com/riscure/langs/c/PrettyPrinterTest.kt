package com.riscure.langs.c

import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.BinaryOp.OpAdd
import com.riscure.bumper.ast.BinaryOp.OpMul
import com.riscure.bumper.ast.Exp.Companion.assign
import com.riscure.bumper.ast.Exp.Companion.dot
import com.riscure.bumper.ast.Exp.Companion.index
import com.riscure.bumper.ast.Exp.Companion.op
import com.riscure.bumper.ast.Exp.Companion.sizeOf
import com.riscure.bumper.ast.Exp.Var
import com.riscure.bumper.pp.Pretty
import kotlin.test.Test
import kotlin.test.assertEquals

class PrettyPrinterTest {

    @Test
    fun variable() = assertEquals(
        "x",
        Pretty.exp(Exp.Var("x", Type.uint))
    )

    @Test
    fun `sum-of-mul`() = assertEquals(
        "x + y * z",
        Pretty.exp(
            op(OpAdd,
               Var("x", Type.uint),
               op(OpMul, Var("y", Type.uint), Var("z", Type.uint), Type.uint),
               Type.uint
            )
        )
    )

    @Test
    fun `mul-of-sum`() = assertEquals(
        "x * (y + z)",
        Pretty.exp(
            op(OpMul,
               Var("x", Type.uint),
               op(OpAdd, Var("y", Type.uint), Var("z", Type.uint), Type.uint),
               Type.uint
            )
        )
    )

    @Test
    fun `mul-of-left-assoc-sum`() = assertEquals(
        "x * (y + z + u)",
        Pretty.exp(
            op(OpMul,
               Var("x", Type.uint),
               op(OpAdd, op(OpAdd, Var("y", Type.uint), Var("z", Type.uint), Type.uint), Var("u", Type.uint), Type.uint),
               Type.uint
            )
        )
    )

    @Test
    fun `mul-of-right-assoc-sum`() = assertEquals(
        "x * (y + (z + u))",
        Pretty.exp(
            op(OpMul,
               Var("x", Type.uint),
               op(OpAdd, Var("y", Type.uint), op(OpAdd, Var("z", Type.uint), Var("u", Type.uint), Type.uint), Type.uint),
               Type.uint
            )
        )
    )

    @Test
    fun assignment() = assertEquals(
        "x = y + z",
        Pretty.exp(
            assign(
                Var("x", Type.uint),
                op(OpAdd, Var("y", Type.uint), Var("z", Type.uint), Type.uint)
            )
        )
    )

    @Test
    fun `member-assignment`() = assertEquals(
        "x.m = y + z",
        Pretty.exp(
            assign(
                dot(Var("x", Type.uint), Field.Named("m", Type.uint)),
                op(OpAdd, Var("y", Type.uint), Var("z", Type.uint), Type.uint)
            )
        )
    )

    @Test
    fun `sizeof-longlong-array`() = assertEquals(
        "sizeof(long long [100])",
        Pretty.exp(
            sizeOf(Type.array(Type.longlong, 100), Stdlib.x64Linux.size_t)
        )
    )

    @Test
    fun `indexing`() = assertEquals(
        "x[i]",
        Pretty.exp(
            index(Var("x", Type.array(Type.uint)), Var("i", Type.uint), Type.uint)
        )
    )

    @Test
    fun `named-attribute`() = assertEquals(
        "__attribute__((no_sanitize(\"coverage\"))) void f()",
        Pretty.declaration("f", Type
            .function(Type.void)
            .withAttrs(Attr.noSanitizeCoverage)
        )
    )

}