package com.palmlang.palm.ast.expressions

import com.palmlang.palm.util.PString

sealed class Pattern {
    data class Expr(val expr: PExpr) : Pattern()
    data class Type(val type: PType, val inverted: Boolean, val destructuring: Destructuring?) : Pattern()
    data class Tuple(val patterns: List<Pattern>) : Pattern()
    data class Dec(val decPattern: PDecPattern) : Pattern()
    data class Parenthesized(val nested: Pattern) : Pattern()
    object Wildcard : Pattern()
}

sealed class Destructuring {
    data class Components(val components: List<Pattern>) : Destructuring()
    data class Object(val properties: List<Pair<PString, Pattern>>) : Destructuring()
}