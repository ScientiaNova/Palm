package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

typealias PDecPattern = Positioned<DecPattern>

sealed class DecPattern {
    data class Mut(val pattern: PDecPattern) : DecPattern()
    data class Name(val name: String) : DecPattern()
    data class Components(val elements: List<PDecPattern>) : DecPattern()
    data class Object(val elements: List<Pair<PString, PDecPattern?>>) : DecPattern()
    object Wildcard : DecPattern()
}