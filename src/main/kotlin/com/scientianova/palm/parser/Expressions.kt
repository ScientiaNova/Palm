package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Expression : IStatement
typealias PExpression = Positioned<Expression>

data class VarExpr(
    val name: String
) : Expression()

data class CallExpr(
    val name: PString,
    val params: List<PExpression>
) : Expression()

data class LambdaExpr(
    val scope: FunctionScope,
    val params: List<PDecPattern>
) : Expression()

data class IfExpr(
    val conditions: PExpression,
    val thenScope: FunctionScope,
    val elseScope: FunctionScope?
) : Expression()

data class IfLetExpr(
    val conditions: PPattern,
    val thenScope: FunctionScope,
    val elseScope: FunctionScope?
) : Expression()

data class WhenExpr(
    val branches: List<Pair<PExpression, FunctionScope>>,
    val elseBranch: FunctionScope?
) : Expression()

data class WhenSwitchExpr(val branches: List<Pair<List<PPattern>, FunctionScope>>) : Expression()

data class ScopeExpr(val scope: FunctionScope) : Expression()
data class ByteExpr(val value: Byte) : Expression()
data class ShortExpr(val value: Short) : Expression()
data class IntExpr(val value: Int) : Expression()
data class LongExpr(val value: Long) : Expression()
data class FloatExpr(val value: Float) : Expression()
data class DoubleExpr(val value: Double) : Expression()
data class BoolExpr(val value: Boolean) : Expression()
data class CharExpr(val value: Char) : Expression()

object UnitExpr : Expression()
object WildcardExpr : Expression()

sealed class StringTemplateComponent
data class StringStringComponent(val string: String) : StringTemplateComponent()
data class ExprStringComponent(val expr: PExpression) : StringTemplateComponent()

data class PureStringExpr(val string: String) : Expression()
data class StringTemplateExpr(val components: List<StringTemplateComponent>)

data class ListExpr(
    val components: List<PExpression>,
    val mutable: Boolean
) : Expression()

data class ArrayExpr(
    val components: List<PExpression>,
    val obj: Boolean
) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String, val negated: Boolean) : BinOp()

data class UnaryOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class BinaryOpsExpr(val body: List<Pair<PExpression, PBinOp>>, val end: PExpression) : Expression()

data class TypeExpr(val expr: PExpression, val type: PType) : Expression()