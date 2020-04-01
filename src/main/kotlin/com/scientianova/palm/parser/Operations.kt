package com.scientianova.palm.parser

import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.callVirtual

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

data class Cast(val expr: IExpression, val type: List<String>) : IExpression {
    override fun evaluate(scope: Scope) = expr.handleForType(scope.getType(type).clazz, scope)
}

data class TypeCheck(val expr: IExpression, val type: List<String>) : IExpression {
    override fun evaluate(scope: Scope) = scope.getType(type).clazz.isInstance(expr.evaluate(scope))
}

data class EqualityCheck(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = first.evaluate(scope) == second.evaluate(scope)
}

data class UnaryOp(val op: UnaryOperation, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = expr.evaluate(scope).callVirtual(op.name, scope)
}

data class BinaryOp(val op: BinaryOperation, val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = first.evaluate(scope).callVirtual(op.name, scope, second.evaluate(scope))
}

data class MultiOp(val op: MultiOperation, val first: IExpression, val rest: List<IExpression>) : IExpression {
    override fun evaluate(scope: Scope) =
        first.evaluate(scope).callVirtual(op.name, scope, rest.map { it.evaluate(scope) })
}

data class Comparison(val type: ComparisonType, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = type.handle(expr.evaluate(scope) as Int)
}

sealed class Operation(val name: String) {
    override fun toString() = name
}

sealed class UnaryOperation(name: String) : Operation(name)
sealed class BinaryOperation(name: String) : Operation(name)
sealed class MultiOperation(name: String) : Operation(name)

object UnaryPlus : UnaryOperation("unary_plus")
object UnaryMinus : UnaryOperation("unary_minus")
object Not : UnaryOperation("not")
object Inv : UnaryOperation("inv")

object Or : BinaryOperation("or")
object And : BinaryOperation("and")
object CompareTo : BinaryOperation("compare_to")
object Shl : BinaryOperation("shl")
object Shr : BinaryOperation("shr")
object Ushr : BinaryOperation("ushr")
object Contains : BinaryOperation("contains")
object Plus : BinaryOperation("plus")
object Minus : BinaryOperation("minus")
object Mul : BinaryOperation("mul")
object Div : BinaryOperation("div")
object Rem : BinaryOperation("rem")
object FloorDiv : BinaryOperation("floor_div")
object Pow : BinaryOperation("times")

object RangeTo : MultiOperation("range_to")
object Get : MultiOperation("get")

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