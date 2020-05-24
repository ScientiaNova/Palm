package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Pattern
typealias PPattern = Positioned<Pattern>

object WildcardPattern : Pattern()
object UnitPattern : Pattern()

data class TuplePattern(
    val components: List<PPattern>
) : Pattern()

data class ExpressionPattern(
    val expr: Expression
) : Pattern()

data class DeclarationPattern(
    val name: PString,
    val type: PType?,
    val mutable: Boolean
) : Pattern()

sealed class DecPattern
typealias PDecPattern = Positioned<DecPattern>

object DecWildcardPattern : DecPattern()
data class DecNamePattern(val name: PString, val mutable: Boolean) : DecPattern()
data class DecTuplePattern(val values: List<PDecPattern>) : DecPattern()