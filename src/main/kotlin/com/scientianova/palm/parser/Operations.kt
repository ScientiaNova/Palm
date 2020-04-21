package com.scientianova.palm.parser

import com.scientianova.palm.evaluator.Scope

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

data class RefEqualityCheck(val first: IExpression, val second: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = first.evaluate(scope) === second.evaluate(scope)
}

data class Comparison(val type: ComparisonType, val expr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) = type.handle(expr.evaluate(scope) as Int)
}

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