package com.scientianovateam.palm.parser

data class Elvis(val first: IExpression, val second: IExpression) : IExpression
data class Conjunction(val first: IExpression, val second: IExpression) : IExpression
data class Disjunction(val first: IExpression, val second: IExpression) : IExpression
data class Cast(val expr: IExpression, val type: IType) : IExpression
data class TypeCheck(val expr: IExpression, val type: IType) : IExpression
data class UnaryOp(val op: UnaryOperation, val expr: IExpression) : IExpression
data class BinaryOp(val op: BinaryOperation, val first: IExpression, val second: IExpression) : IExpression
data class MultiOp(val op: MultiOperation, val first: IExpression, val rest: List<IExpression>) : IExpression
data class Comparison(val type: ComparisonType, val expr: IExpression) : IExpression

sealed class Operation
sealed class UnaryOperation : Operation()
sealed class BinaryOperation : Operation()
sealed class MultiOperation : Operation()

object UnaryPlus : UnaryOperation()
object UnaryMinus : UnaryOperation()
object Not : UnaryOperation()

object Compare : BinaryOperation()
object Equals : BinaryOperation()
object Contains : BinaryOperation()
object Add : BinaryOperation()
object Sub : BinaryOperation()
object Mul : BinaryOperation()
object Div : BinaryOperation()
object Rem : BinaryOperation()
object FloorDiv : BinaryOperation()
object Pow : BinaryOperation()

object ToRange : MultiOperation()
object Get : MultiOperation()

enum class ComparisonType { L, LE, G, GE }