package com.scientianovateam.palm.parser

import com.scientianovateam.palm.evaluator.Scope
import com.scientianovateam.palm.evaluator.palmType

data class Elvis(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) ?: second.evaluate(scope)

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + second.find(type, predicate)
}

data class Conjunction(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) == true && second.evaluate(scope) == false

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + second.find(type, predicate)
}

data class Disjunction(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) == true || second.evaluate(scope) == false

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + second.find(type, predicate)
}

data class Cast(val expr: IExpression, val type: Class<*>) : IExpression {
    override fun evaluate(scope: Scope) = expr.handleForType(type, scope)

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
}

data class TypeCheck(val expr: IExpression, val type: Class<*>) : IExpression {
    override fun evaluate(scope: Scope) = type.isInstance(expr.evaluate(scope))

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
}

data class EqualityCheck(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = first.evaluate(scope) == second.evaluate(scope)

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + second.find(type, predicate)
}

data class UnaryOp(val op: UnaryOperation, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope)
        return value.palmType.execute(op, value)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
}

data class BinaryOp(val op: BinaryOperation, val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val firstValue = first.evaluate(scope)
        val secondValue = second.evaluate(scope)
        return firstValue.palmType.execute(op, firstValue, secondValue)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + second.find(type, predicate)
}

data class MultiOp(val op: MultiOperation, val first: IExpression, val rest: List<IExpression>) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val firstValue = first.evaluate(scope)
        val otherValues = rest.map { it.evaluate(scope) }
        return firstValue.palmType.execute(op, firstValue, otherValues)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + first.find(type, predicate) + rest.flatMap { it.find(type, predicate) }
}

data class Comparison(val type: ComparisonType, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = type.handle(expr.evaluate(scope) as Int)

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
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
    "compareTo" to CompareTo,
    "contains" to Contains,
    "plus" to Plus,
    "minus" to Minus,
    "mul" to Mul,
    "div" to Div,
    "floorDiv" to FloorDiv,
    "rem" to Rem,
    "mod" to Rem,
    "pow" to Pow
)

object CompareTo : BinaryOperation("<=>", Int::class.java)
object Contains : BinaryOperation("in", Boolean::class.java)
object Plus : BinaryOperation("+")
object Minus : BinaryOperation("-")
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