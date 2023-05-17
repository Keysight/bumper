package com.riscure.bumper.ast


/** Float contants */
data class FloatConstant(
    val hex: Boolean,
    val intPart: String,
    val fracPart: String,
    val exponent: String
)

/** A model of syntax for integer and float constants */
sealed interface Constant {
    data class CInt(val value: Long, val kind: IKind): Constant
    data class CFloat(val value: FloatConstant, val kind: FKind): Constant
    data class CStr(
        /**
         * The value of the string literal.
         *
         * That is: [value] is *not* encoded as a C string literal but is the decoded value of
         * that literal. That is, the [value] for the C string literal "\t" is not "\\t", but "   ",
         * if you catch my drift.
         */
        val value: String
    ): Constant
    // data class CWStr (val value: List<String>): Constant()
    // data class CEnum (val value: Pair<Ref, Int>): Constant()
}

/** A model of C expressions */
sealed interface Exp {
    val etype: Type
    val prec: Prec

    // in order of precedence

    data class Const(
        val constant: Constant,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.na(16)
    }

    data class Var(
        val name: Ident,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.na(16)
    }

    data class Sizeof(
        val type: Type,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(15)
    }

    data class Alignof(
        val type : Type,
        override val etype : Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(15)
    }

    // compound literals
    // - (Point) { .x = 1; .y = 2; }
    data class Compound(
        val initializer: Initializer.Compound,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(14)
    }

    data class Cast(
        val toType: Type,
        val exp: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(14)
    }

    data class UnOp(
        val op: UnaryOp,
        val operand: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = op.prec
    }

    data class BinOp(
        val op: BinaryOp,
        val left: Exp,
        val right: Exp,
        val atType: Type,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = op.prec
    }

    data class Conditional(
        val condition: Exp,
        val thenBranch: Exp,
        val elseBranch: Exp,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.rl(3)
    }

    data class Call(
        val funRef: Exp,
        val args: List<Exp>,
        override val etype: Type
    ): Exp {
        override val prec: Prec get() = Prec.lr(16)
    }

    companion object {
        @JvmStatic
        fun constant(i: Long, kind: IKind) = Const(Constant.CInt(i, kind), Type.Int(kind))
        @JvmStatic
        fun cast(to: Type, exp: Exp) = Cast(to, exp, to)
        @JvmStatic
        fun not(exp: Exp) = UnOp(UnaryOp.OpLogNot, exp, Type.bool)
        @JvmStatic
        fun dot(exp: Exp, field: Field.Named) = UnOp(UnaryOp.OpDot(field.name), exp, field.type)
        @JvmStatic
        fun index(lhs: Exp, rhs: Exp, elTyp: Type) = BinOp(BinaryOp.OpIndex, lhs, rhs, lhs.etype, elTyp)
        @JvmStatic
        fun arrow(exp: Exp, field: Field.Named) = UnOp(UnaryOp.OpArrow(field.name), exp, field.type)

        @JvmStatic
        fun deref(exp: Exp, pointee: Type) = when {
            exp is UnOp && exp.op is UnaryOp.OpAddrOf -> exp.operand
            else                                      -> UnOp(UnaryOp.OpDeref, exp, pointee)
        }
        @JvmStatic
        fun addrOf(exp: Exp) = when {
            exp is UnOp && exp.op is UnaryOp.OpDeref -> exp.operand
            else                                     -> UnOp(UnaryOp.OpAddrOf, exp, exp.etype.ptr())
        }

        @JvmStatic
        fun sizeOf(typ: Type, sizeKind: IKind) = Sizeof(typ, Type.Int(sizeKind))

        @JvmStatic
        fun assign(lhs: Exp, rhs: Exp) = BinOp(BinaryOp.OpAssign, lhs, rhs, lhs.etype, lhs.etype)

        @JvmStatic
        fun op(op: BinaryOp, lhs: Exp, rhs: Exp, result: Type) = BinOp(op, lhs, rhs, lhs.etype, result)

        @JvmStatic
        fun call(function: Exp, vararg args: Exp) = when (val typ = function.etype) {
            is Type.Fun -> Call(function, args.toList(), typ.returnType)
            else -> throw RuntimeException("Expected expression with function type, got $typ")
        }

        @JvmStatic
        fun struct(struct: TypeRef, initializers: Map<Ident, Initializer>) =
            Compound(Initializer.InitStruct(struct, initializers), Type.Struct(struct))

        @JvmStatic
        fun union(union: TypeRef, designator: Ident, initializer: Initializer) =
            Compound(Initializer.InitUnion(union, designator, initializer), Type.Struct(union))
    }
}

// data class AsmOperand(val wut: Option<String>, val wat: Exp)
// typealias AsmOperands = List<AsmOperand>
