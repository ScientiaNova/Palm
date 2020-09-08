package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.parser.data.types.PType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

private typealias Expression = Expr

typealias PPattern = Positioned<Pattern>
sealed class Pattern {
    data class Expr(val expr: Expression) : Pattern()
    data class Dec(val name: String, val mutable: Boolean) : Pattern()
    data class Enum(val name: PString, val params: List<PPattern>) : Pattern()
    data class Tuple(val elements: List<PPattern>) : Pattern()
    data class Record(val pairs: List<Pair<PString, PPattern>>) : Pattern()
    data class Type(val type: PType) : Pattern()
    object Wildcard : Pattern()
}