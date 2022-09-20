// This is a fully type-annotated C source AST.
// The representation is ported from the CompCert CParser elaborated AST.
package com.riscure.langs.c

import java.util.*

typealias Name  = String
typealias Ident = String

enum class IKind {
      IBoolean
    , IChar, ISChar, IUChar
    , IShort, IUShort
    , IInt, IUInt
    , ILong, IULong
    , ILongLong, IULongLong
}

enum class FKind {
    FFloat, FDouble, FLongDouble
}

data class FloatConstant
  ( val hex: Boolean
  , val intPart: String
  , val fracPart: String
  , val exp: String
  )

sealed class Constant<T> {
    abstract val value: T
}
data class CInt  (override val value: Int, val kind: IKind): Constant<Int>()
data class CFloat(override val value: FloatConstant, val kind: FKind): Constant<FloatConstant>()
data class CStr  (override val value: String): Constant<String>()
data class CWStr (override val value: List<String>): Constant<List<String>>()
data class CEnum (override val value: Pair<Ident, Int>): Constant<Pair<Ident, Int>>()

/* Attributes */
sealed class AttrArg
data class AIdent  (val value: String): AttrArg()
data class AInt    (val value: Int): AttrArg()
data class AString (val value: String): AttrArg()

sealed class Attr
object AConstant: Attr()
object AVolatile: Attr()
object ARestrict: Attr()
data class AAlignAs (val alignment: Int): Attr()
data class AAttr    (val name: String, val args: List<AttrArg>): Attr()

typealias Attrs = List<Attr>

/* Types */
sealed class Type(private val _attrs: Attrs) {
    fun attrs() = _attrs

    data class Void   (val attrs: Attrs = listOf()): Type(attrs)
    data class Int    (val kind: IKind, val attrs: Attrs = listOf()): Type(attrs)
    data class Float  (val kind: FKind, val attrs: Attrs = listOf()): Type(attrs)
    data class Ptr    (val type: Type, val attrs: Attrs = listOf()): Type(attrs)
    data class Array  (val type: Type, val size: Optional<Long> = Optional.empty(), val attrs: Attrs = listOf()): Type(attrs)
    data class Fun    (val retType: Type, val args: List<Pair<Ident, Type>>, val vararg: Boolean, val attrs: Attrs = listOf()): Type(attrs)
    data class Named  (val id: Ident, val attrs: Attrs = listOf()): Type(attrs)
    data class Struct (val id: Ident, val attrs: Attrs = listOf()): Type(attrs)
    data class Union  (val id: Ident, val attrs: Attrs = listOf()): Type(attrs)
    data class Enum   (val id: Ident, val attrs: Attrs = listOf()): Type(attrs)
}

/* Struct or union field */
data class Field(
    val name: String
  , val type: Type
  , val bitfield: Int?
  , val anonymous: Boolean
)

enum class StructOrUnion { Struct, Union }

/* Type-annotated expression */
data class TypedExp(val exp: Exp, val type: Type)

typealias Local = String

sealed class Exp
//data class EConst(val constant: Constant): Exp()
//data class ESizeof(val type: Type): Exp()
//data class EAlignof(val type : Type): Exp()
//data class EVar(val name: Local): Exp()
//data class EUnop(val op: UnOp, val exp: Exp): Exp()
//data class EBinop(val op: BinOp, val left: Exp, val right: Exp, val atType: Type): Exp()
//data class EConditional(val condition: Exp, val thenBranch: Exp, val elseBranch: Exp): Exp()
//data class ECast(val toType: Type, val exp: Exp): Exp()
//data class ECompound(val type: Type, val initializer: Initializer): Exp()
//data class ECall(val funRef: Exp, val args: List<Exp>): Exp()
//
//typealias FieldInitializer = Pair<Field, Initializer>
//
sealed class Initializer {
    data class InitSingle(val exp: Exp): Initializer()
    data class InitArray(val exps: List<Exp>): Initializer()
    // data class InitStruct(val struct: Ident, val fieldInitializers: List<FieldInitializer>): Initializer()
    // data class InitUnion(val union: Ident, val fieldInitializer: FieldInitializer): Initializer()
}

//(** GCC extended asm *)
//
//type asm_operand = string option * string * exp

/* Statements */
sealed class Stmt {
    data class Decl(val name: Name, val type: Type, val init: Optional<Initializer> = Optional.empty()): Stmt()/* val storage: Storage */
    data class Block(val stmts: List<Stmt>): Stmt()
}
//object Sskip: Stmt()
//data class Sdo(val todo: Exp): Stmt()
//data class Sseq(val first: Stmt, val snd: Stmt): Stmt()
//data class Sif(val condition: Exp, val thenBranch: Stmt, val elseBranch: Stmt): Stmt()
//data class Swhile(val condition: Exp, val body: Stmt): Stmt()
//data class Sdowhile(val body: Stmt, val condition: Exp): Stmt()
//data class Sfor(val before: Stmt, val condition: Exp, val after: Stmt, val body: Stmt): Stmt()
//object     Sbreak: Stmt()
//object     Scontinue: Stmt()
//data class Sswitch(val scrutinee: Exp, val body: Stmt): Stmt()
//data class Slabeled(val label: StmtLabel, val stmt: Stmt): Stmt()
//data class Sgoto(val label: Label): Stmt()
//data class Sreturn(val value: Exp?): Stmt()
//// data class Sasm of attributes * string * asm_operand list * asm_operand list * string list(): Stmt
//
//sealed class StmtLabel
//data class Label(val label: String): StmtLabel()
//data class Case(val case: Exp, val num: Int): StmtLabel()
//object DefaultCase: StmtLabel()

//(** Element of an enumeration *)
//
//type enumerator = ident * int64 * exp option

data class Param(
    val name: Ident,
    val type: Type
)

sealed class GlobalDecl {
    data class Fun(
        val inline: Boolean,
        val name: String,
        val ret: Type,
        val params: List<Param>,
        val vararg: Boolean,
        val locals: List<Stmt.Decl>,
        val body: Stmt
    ) : GlobalDecl()
    /* fun possibly missing attributes and and storage fields */
}
//  | Gdecl of decl           (* variable declaration, function prototype *)
//  | Gfundef of fundef                   (* function definition *)
//  | Gcompositedecl of struct_or_union * ident * attributes
//                                        (* struct/union declaration *)
//  | Gcompositedef of struct_or_union * ident * attributes * field list
//                                        (* struct/union definition *)
//  | Gtypedef of ident * typ             (* typedef *)
//  | Genumdef of ident * attributes * enumerator list
//                                        (* enum definition *)
//  | Gpragma of string                   (* #pragma directive *)
//

data class TranslationUnit(
    val decls: List<GlobalDecl>
)

//(** Builtin types and functions *)
//
//type builtins = {
//  builtin_typedefs: (string * typ) list;
//  builtin_functions: (string * (typ * typ list * bool)) list
//}
