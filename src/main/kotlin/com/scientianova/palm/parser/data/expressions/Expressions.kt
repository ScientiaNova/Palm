package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.lexer.StringPart
import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.parser.data.types.ObjectStatement
import com.scientianova.palm.parser.data.types.SuperType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

typealias PExpr = Positioned<Expr>

sealed class Expr {
    data class Ident(val name: String) : Expr()
    data class Call(val expr: PExpr, val args: CallArgs) : Expr()
    data class Lambda(val label: PString?, val params: LambdaParams, val scope: ExprScope) : Expr()

    data class If(val cond: List<Condition>, val ifTrue: ExprScope, val ifFalse: ExprScope?) : Expr()
    data class When(val comparing: PExpr?, val branches: List<WhenBranch>) : Expr()

    data class For(
        val label: PString?,
        val dec: PDecPattern,
        val iterable: PExpr,
        val body: ExprScope,
        val noBreak: ExprScope?
    ) : Expr()

    data class While(
        val label: PString?,
        val cond: List<Condition>,
        val body: ExprScope,
        val noBreak: ExprScope?
    ) : Expr()

    data class Loop(val label: PString?, val body: ExprScope) : Expr()

    data class Continue(val label: PString?) : Expr()
    data class Break(val label: PString?, val expr: PExpr?) : Expr()
    data class Return(val label: PString?, val expr: PExpr?) : Expr()

    data class Throw(val expr: PExpr) : Expr()

    data class Do(val scope: ExprScope, val catches: List<Catch>) : Expr()

    data class Scope(val scope: ExprScope) : Expr()

    data class Byte(val value: kotlin.Byte) : Expr()
    data class Short(val value: kotlin.Short) : Expr()
    data class Int(val value: kotlin.Int) : Expr()
    data class Long(val value: kotlin.Long) : Expr()
    data class Float(val value: kotlin.Float) : Expr()
    data class Double(val value: kotlin.Double) : Expr()
    data class Char(val value: kotlin.Char) : Expr()
    data class Str(val parts: List<StringPart>) : Expr()
    data class Bool(val value: Boolean) : Expr()

    object Null : Expr()
    object Wildcard : Expr()

    object This : Expr()
    object Super : Expr()

    data class Tuple(val elements: List<PExpr>) : Expr()
    data class Lis(val elements: List<PExpr>) : Expr()
    data class Map(val elements: List<Pair<PExpr, PExpr>>) : Expr()

    data class Get(val expr: PExpr, val args: List<PExpr>) : Expr()

    data class TypeCheck(val expr: PExpr, val type: PType) : Expr()
    data class SafeCast(val expr: PExpr, val type: PType) : Expr()
    data class NullableCast(val expr: PExpr, val type: PType) : Expr()
    data class UnsafeCast(val expr: PExpr, val type: PType) : Expr()

    data class MemberAccess(val expr: PExpr, val value: PString) : Expr()
    data class SafeMemberAccess(val expr: PExpr, val value: PString) : Expr()

    data class Turbofish(val expr: PExpr, val args: List<PTypeArg>) : Expr()

    data class FunRef(val on: PExpr?, val value: PString) : Expr()
    data class Spread(val expr: PExpr) : Expr()

    data class Unary(val op: UnaryOp, val expr: PExpr) : Expr()
    data class Binary(val first: PExpr, val op: BinaryOp, val second: PExpr) : Expr()

    data class And(val first: PExpr, val second: PExpr) : Expr()
    data class Or(val first: PExpr, val second: PExpr) : Expr()
    data class Elvis(val first: PExpr, val second: PExpr) : Expr()

    data class Eq(val first: PExpr, val second: PExpr) : Expr()
    data class NotEq(val first: PExpr, val second: PExpr) : Expr()

    data class RefEq(val first: PExpr, val second: PExpr) : Expr()
    data class NotRefEq(val first: PExpr, val second: PExpr) : Expr()

    data class Dec(val mutable: Boolean, val pattern: PExpr, val type: PType?, val expr: PExpr?) : Expr()
    data class Assign(val left: PExpr, val right: PExpr, val type: AssignmentType) : Expr()

    data class Guard(val cond: List<Condition>, val body: ExprScope) : Expr()
    data class Defer(val body: ExprScope) : Expr()

    data class Annotated(val annotation: Annotation, val expr: PExpr) : Expr()

    data class Object(
        val superTypes: List<SuperType>,
        val statements: List<ObjectStatement>
    ) : Expr()
}

sealed class Arg {
    data class Free(val value: PExpr) : Arg()
    data class Named(val name: PString, val value: PExpr) : Arg()
}

data class CallArgs(val args: List<Arg> = emptyList(), val last: PExpr? = null)

data class Catch(val dec: PDecPattern, val type: PType, val body: ExprScope)

typealias LambdaParams = List<Pair<PDecPattern, PType?>>

sealed class Condition {
    data class Expr(val expr: PExpr) : Condition()
    data class Pattern(val mutable: Boolean, val pattern: PExpr, val expr: PExpr) : Condition()
}

typealias WhenBranch = Pair<Pattern, PExpr>

typealias ExprScope = List<PExpr>