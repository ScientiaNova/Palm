package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned

typealias PDecPattern = Positioned<DecPattern>

sealed class DecPattern : ASTNode {
    data class Mut(val pattern: PDecPattern) : DecPattern()
    data class Name(val name: String) : DecPattern()
    data class Components(val elements: List<PDecPattern>) : DecPattern()
    data class Object(val elements: List<Pair<PString, PDecPattern?>>) : DecPattern()
    data class Parenthesized(val nested: PDecPattern) : DecPattern()
    object Wildcard : DecPattern()
}