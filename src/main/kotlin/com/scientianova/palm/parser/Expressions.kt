package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Expression
typealias PExpr = Positioned<Expression>

data class IdentExpr(val name: String) : Expression()
data class OpRefExpr(val symbol: String) : Expression()

sealed class Arg {
    data class Free(val value: PExpr) : Arg()
    data class Named(val name: PString, val value: PExpr) : Arg()
}

data class CallArgs(val args: List<Arg> = emptyList(), val last: PExpr? = null)

data class CallExpr(
    val expr: PExpr,
    val args: CallArgs
) : Expression()

typealias LambdaParams = List<Pair<PString, PType?>>

data class LambdaExpr(
    val params: LambdaParams,
    val scope: ExprScope
) : Expression()

sealed class Condition
data class ExprCondition(val expr: PExpr) : Condition()
data class DecCondition(val pattern: PPattern, val expr: PExpr) : Condition()

data class IfExpr(
    val cond: List<Condition>,
    val ifTrue: ExprScope,
    val ifFalse: ExprScope?
) : Expression()

typealias WhenBranch = Pair<PPattern, PExpr>

data class WhenExpr(
    val comparing: PExpr?,
    val branches: List<WhenBranch>
) : Expression()

data class ForExpr(
    val name: PString,
    val iterable: PExpr,
    val body: ExprScope
) : Expression()

data class ScopeExpr(val scope: ExprScope) : Expression()

data class ByteExpr(val value: Byte) : Expression()
data class ShortExpr(val value: Short) : Expression()
data class IntExpr(val value: Int) : Expression()
data class LongExpr(val value: Long) : Expression()
data class FloatExpr(val value: Float) : Expression()
data class DoubleExpr(val value: Double) : Expression()
data class CharExpr(val value: Char) : Expression()
data class StringExpr(val string: String) : Expression()
data class BoolExpr(val value: Boolean) : Expression()

val trueExpr = BoolExpr(true)
val falseExpr = BoolExpr(false)

val emptyString = StringExpr("")

object NullExpr : Expression()
object ThisExpr : Expression()

data class ListExpr(val components: List<PExpr>) : Expression()