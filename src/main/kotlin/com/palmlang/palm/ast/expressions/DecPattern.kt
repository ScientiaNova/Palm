package com.palmlang.palm.ast.expressions

import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned

typealias PDecPattern = Positioned<com.palmlang.palm.ast.expressions.DecPattern>

sealed class DecPattern {
    data class Mut(val pattern: com.palmlang.palm.ast.expressions.PDecPattern) : com.palmlang.palm.ast.expressions.DecPattern()
    data class Name(val name: String) : com.palmlang.palm.ast.expressions.DecPattern()
    data class Components(val elements: List<com.palmlang.palm.ast.expressions.PDecPattern>) : com.palmlang.palm.ast.expressions.DecPattern()
    data class Object(val elements: List<Pair<PString, com.palmlang.palm.ast.expressions.PDecPattern?>>) : com.palmlang.palm.ast.expressions.DecPattern()
    data class Parenthesized(val nested: com.palmlang.palm.ast.expressions.PDecPattern) : com.palmlang.palm.ast.expressions.DecPattern()
    object Wildcard : com.palmlang.palm.ast.expressions.DecPattern()
}