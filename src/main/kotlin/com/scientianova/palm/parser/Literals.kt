package com.scientianova.palm.parser

data class ByteExpr(
    val value: Byte
) : IExpression

data class ShortExpr(
    val value: Short
) : IExpression

data class IntExpr(
    val value: Int
) : IExpression

data class LongExpr(
    val value: Long
) : IExpression

data class FloatExpr(
    val value: Float
) : IExpression

data class DoubleExpr(
    val value: Double
) : IExpression

data class BoolExpr(
    val value: Boolean
) : IExpression

object NullExpr : IExpression

sealed class StringTemplateComponent
data class StringStringComponent(val string: String) : StringTemplateComponent()
data class ExprStringComponent(val expr: PExpression) : StringTemplateComponent()

data class PureStringExpr(val string: String) : IExpression
data class StringTemplateExpr(val components: List<StringTemplateComponent>)

sealed class ListComponent
data class ExprListComponent(val expr: PExpression) : ListComponent()
data class SpreadListComponent(val expr: PExpression) : ListComponent()

sealed class MapComponent
data class PairMapComponent(val key: PExpression, val value: PExpression) : ListComponent()
data class SpreadMapComponent(val expr: PExpression) : ListComponent()

data class ListExpr(
    val components: List<ListComponent>
) : IExpression

data class ArrayExpr(
    val components: List<ListComponent>
) : IExpression

data class MapExpr(
    val components: List<MapComponent>
) : IExpression

data class ListCompExpr(
    val declaration: PPattern,
    val iterableExpr: PExpression,
    val forExpr: PExpression,
    val condition: PExpression?
) : IExpression

data class ArrayCompExpr(
    val declaration: PPattern,
    val iterableExpr: PExpression,
    val forExpr: PExpression,
    val condition: PExpression?
) : IExpression

data class MapCompExpr(
    val declaration: PPattern,
    val iterableExpr: PExpression,
    val keyExpr: PExpression,
    val valueExpr: PExpression,
    val condition: PExpression?
) : IExpression