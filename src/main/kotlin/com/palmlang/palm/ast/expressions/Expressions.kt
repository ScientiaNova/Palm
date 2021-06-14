package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned

typealias PExpr = Positioned<Expr>

sealed class Expr : ASTNode {
    object Error : Expr()
    data class Ident(val name: String) : Expr()

    data class Safe(val expr: Expr) : Expr()
    data class Call(val expr: PExpr, val args: CallArgs) : Expr()
    data class Lambda(val scope: Scope) : Expr()

    data class Ternary(val cond: PExpr, val ifTrue: PExpr, val ifFalse: PExpr) : Expr()
    data class When(val comparing: PExpr?, val branches: List<WhenBranch>) : Expr()

    object Super : Expr()
    data class Return(val label: PString?, val expr: PExpr?) : Expr()

    data class Byte(val value: kotlin.Byte) : Expr()
    data class Short(val value: kotlin.Short) : Expr()
    data class Int(val value: kotlin.Int) : Expr()
    data class Long(val value: kotlin.Long) : Expr()
    data class Float(val value: kotlin.Float) : Expr()
    data class Double(val value: kotlin.Double) : Expr()
    data class Char(val value: kotlin.Char) : Expr()
    data class Str(val parts: List<StringPartP>) : Expr()
    data class Bool(val value: Boolean) : Expr()
    object Null : Expr()

    data class Parenthesized(val nested: PExpr) : Expr()
    data class Tuple(val elements: List<PExpr>) : Expr()
    data class Lis(val elements: List<PExpr>) : Expr()
    data class Map(val elements: List<Pair<PExpr, PExpr>>) : Expr()

    data class Get(val expr: PExpr, val arg: PExpr) : Expr()

    data class TypeCheck(val expr: PExpr, val type: PType, val destructuring: Destructuring?, val not: Boolean) : Expr()
    data class TypeInfo(val expr: PExpr, val type: PType) : Expr()
    data class NullableCast(val expr: PExpr, val type: PType) : Expr()
    data class UnsafeCast(val expr: PExpr, val type: PType) : Expr()

    data class MemberAccess(val expr: PExpr, val value: PString) : Expr()
    data class Turbofish(val expr: PExpr, val args: List<Arg<PType>>) : Expr()
    data class ContextCall(val expr: PExpr, val args: List<Arg<PExpr>>) : Expr()

    data class Unary(val op: Positioned<UnOp>, val expr: PExpr) : Expr()
    data class Binary(val op: Positioned<ExprOp>, val first: PExpr, val second: PExpr) : Expr()

    data class FunRef(val on: PExpr?, val value: PString) : Expr()
    data class Spread(val expr: PExpr) : Expr()

    object Module : Expr()
}

sealed class StringPartP {
    data class String(val string: kotlin.String) : StringPartP()
    data class Expr(val scope: PScope) : StringPartP()
}

data class Arg<T>(val name: PString?, val value: T)
data class CallArgs(val args: List<Arg<PExpr>> = emptyList(), val trailing: List<Arg<PExpr>> = emptyList())

data class LambdaHeader(
    val context: List<Pair<PDecPattern, PType?>>,
    val explicit: List<Pair<PDecPattern, PType?>>,
    val returnType: PType?
)

data class WhenBranch(val pattern: Pattern, val guard: BranchGuard?, val res: BranchRes)
data class BranchGuard(val expr: PExpr)
sealed class BranchRes {
    data class Branching(val on: PExpr?, val branches: List<WhenBranch>) : BranchRes()
    data class Single(val expr: PExpr) : BranchRes()
}