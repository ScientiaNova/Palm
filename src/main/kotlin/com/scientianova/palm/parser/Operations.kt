package com.scientianova.palm.parser

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.palmType

data class Elvis(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope) ?: second.evaluate(scope)
}

data class Walrus(val name: String, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        expr.evaluate(scope).also { scope[name] = it }
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
    "not" to Not,
    "inv" to Inv
)

object UnaryPlus : UnaryOperation("unary -")
object UnaryMinus : UnaryOperation("unary +")
object Not : UnaryOperation("not")
object Inv : UnaryOperation("~")

val BINARY_OPS = mapOf(
    "or" to Or,
    "and" to And,
    "compareTo" to CompareTo,
    "shl" to Shl,
    "shr" to Shr,
    "ushr" to Ushr,
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

object Or : BinaryOperation("|")
object And : BinaryOperation("&")
object CompareTo : BinaryOperation("<=>", Int::class.java)
object Shl : BinaryOperation("<<")
object Shr : BinaryOperation(">>")
object Ushr : BinaryOperation(">>>")
object Contains : BinaryOperation("in", Boolean::class.java)
object Plus : BinaryOperation("+")
object Minus : BinaryOperation("-")
object Mul : BinaryOperation("*")
object Div : BinaryOperation("/")
object Rem : BinaryOperation("%")
object FloorDiv : BinaryOperation("//")
object Pow : BinaryOperation("*")

object RangeTo : MultiOperation("...")
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