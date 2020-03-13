package com.scientianovateam.palm.parser

import com.scientianovateam.palm.evaluator.Scope
import com.scientianovateam.palm.evaluator.palmType

data class Elvis(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) ?: second.evaluate(scope)
}

data class Conjunction(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) == true && second.evaluate(scope) == false
}

data class Disjunction(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) == true || second.evaluate(scope) == false
}

data class Cast(val expr: IExpression, val type: Class<*>) : IExpression {
    override fun evaluate(scope: Scope) = expr.handleForType(type, scope)
}

data class TypeCheck(val expr: IExpression, val type: Class<*>) : IExpression {
    override fun evaluate(scope: Scope) = type.isInstance(expr.evaluate(scope))
}

data class EqualityCheck(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = first.evaluate(scope) == second.evaluate(scope)
}

data class UnaryOp(val op: UnaryOperation, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope)
        return value.palmType.execute(op, value)
    }
}

data class BinaryOp(val op: BinaryOperation, val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val firstValue = first.evaluate(scope)
        val secondValue = second.evaluate(scope)
        return firstValue.palmType.execute(op, firstValue, secondValue)
    }
}

data class MultiOp(val op: MultiOperation, val first: IExpression, val rest: List<IExpression>) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val firstValue = first.evaluate(scope)
        val otherValues = rest.map { it.evaluate(scope) }
        return firstValue.palmType.execute(op, firstValue, otherValues)
    }
}

data class Comparison(val type: ComparisonType, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = type.handle(expr.evaluate(scope) as Int)
}

sealed class Operation(val name: String) {
    override fun toString() = name
}

sealed class UnaryOperation(name: String) : Operation(name)
sealed class BinaryOperation(name: String, val returnType: Class<*>? = null) : Operation(name)
sealed class MultiOperation(name: String) : Operation(name)

val UNARY_OPS = mapOf(
    "unaryPlus" to UnaryPlus,
    "unaryMinus" to UnaryMinus,
    "not" to Not
)

object UnaryPlus : UnaryOperation("unary -")
object UnaryMinus : UnaryOperation("unary +")
object Not : UnaryOperation("not")

val BINARY_OPS = mapOf(
    "compare" to Compare,
    "contains" to Contains,
    "ad" to Add,
    "sub" to Sub,
    "mul" to Mul,
    "div" to Div,
    "rem" to Rem,
    "floorDiv" to FloorDiv,
    "pow" to Pow
)

object Compare : BinaryOperation("<=>", Int::class.java)
object Contains : BinaryOperation("in", Boolean::class.java)
object Add : BinaryOperation("+")
object Sub : BinaryOperation("-")
object Mul : BinaryOperation("*")
object Div : BinaryOperation("/")
object Rem : BinaryOperation("%")
object FloorDiv : BinaryOperation("//")
object Pow : BinaryOperation("*")

object ToRange : MultiOperation("...")
object Get : MultiOperation("[]")

enum class ComparisonType {
    L {
        override fun handle(num: Int) = num < 0
    },
    LE {
        override fun handle(num: Int) = num <= 0
    },
    G {
        override fun handle(num: Int) = num > 0
    },
    GE {
        override fun handle(num: Int) = num >= 0
    };

    abstract fun handle(num: Int): Boolean
}