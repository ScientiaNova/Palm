package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.util.PString

sealed interface Pattern : ASTNode {
    data class Expr(val expr: PExpr) : Pattern
    data class Type(val type: PType, val inverted: Boolean, val destructuring: Destructuring?) : Pattern
    data class Tuple(val patterns: List<Pattern>) : Pattern
    data class Dec(val decPattern: PDecPattern) : Pattern
    data class Parenthesized(val nested: Pattern) : Pattern
}

sealed interface Destructuring {
    data class Components(val components: List<Pattern>) : Destructuring
    data class Object(val properties: List<Pair<PString, Pattern>>) : Destructuring
}