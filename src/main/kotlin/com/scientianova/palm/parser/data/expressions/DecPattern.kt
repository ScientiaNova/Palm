package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

typealias PDecPattern = Positioned<DecPattern>

sealed class DecPattern {
    data class Name(val name: String) : DecPattern()
    data class Tuple(val elements: List<PDecPattern>) : DecPattern()
    data class Record(val pairs: List<Pair<PString, PDecPattern>>) : DecPattern()
    object Wildcard : DecPattern()
}