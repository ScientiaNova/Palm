package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Pattern
typealias PPattern = Positioned<Pattern>

data class ExprPattern(val expr: Expression) : Pattern()
data class DecPattern(val name: String, val mutable: Boolean) : Pattern()
data class EnumPattern(val name: PString, val params: List<PPattern>) : Pattern()
data class TypePattern(val type: PType) : Pattern()
object WildcardPattern : Pattern()