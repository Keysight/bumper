package com.riscure.langs.c

import com.riscure.bumper.ast.*
import com.riscure.bumper.ast.Exp.*
import com.riscure.bumper.ast.BinaryOp.*
import com.riscure.bumper.ast.Exp.Companion.assign
import com.riscure.bumper.ast.Exp.Companion.dot
import com.riscure.bumper.ast.Exp.Companion.op
import com.riscure.bumper.ast.Exp.Companion.sizeOf
import com.riscure.bumper.pp.Pretty
import kotlin.test.*

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
            op(OAdd,
                Var("x", Type.uint),
                op(OMul, Var("y", Type.uint), Var("z", Type.uint))
            )
        )
    )

    @Test
    fun `mul-of-sum`() = assertEquals(
        "x * (y + z)",
        Pretty.exp(
            op(OMul,
                Var("x", Type.uint),
                op(OAdd, Var("y", Type.uint), Var("z", Type.uint))
            )
        )
    )

    @Test
    fun `mul-of-left-assoc-sum`() = assertEquals(
        "x * (y + z + u)",
        Pretty.exp(
            op(OMul,
                Var("x", Type.uint),
                op(OAdd, op(OAdd, Var("y", Type.uint), Var("z", Type.uint)), Var("u", Type.uint))
            )
        )
    )

    @Test
    fun `mul-of-right-assoc-sum`() = assertEquals(
        "x * (y + (z + u))",
        Pretty.exp(
            op(OMul,
                Var("x", Type.uint),
                op(OAdd, Var("y", Type.uint), op(OAdd, Var("z", Type.uint), Var("u", Type.uint)))
            )
        )
    )

    @Test
    fun assignment() = assertEquals(
        "x = y + z",
        Pretty.exp(
            assign(
                Var("x", Type.uint),
                op(OAdd, Var("y", Type.uint), Var("z", Type.uint))
            )
        )
    )

    @Test
    fun `member-assignment`() = assertEquals(
        "x.m = y + z",
        Pretty.exp(
            assign(
                dot(Var("x", Type.uint), Field("m", Type.uint)),
                op(OAdd, Var("y", Type.uint), Var("z", Type.uint))
            )
        )
    )

    @Test
    fun `sizeof-longlong-array`() = assertEquals(
        "sizeof(long long [100])",
        Pretty.exp(
            sizeOf(Type.array(Type.longlong, 100), IKind.IULong)
        )
    )
}