package com.scientianova.palm.parser.top

import com.scientianova.palm.parser.expressions.PExpr
import com.scientianova.palm.parser.types.PType
import com.scientianova.palm.parser.types.ClassLevelPrivacy
import com.scientianova.palm.util.PString

enum class ParamModifier {
    Using, Given
}

data class Function(
    val name: PString,
    val params: List<FunParam>,
    val type: PType?,
    val expr: PExpr
)

data class FunParam(
    val modifier: List<ParamModifier>,
    val name: PString,
    val type: PType,
    val default: PExpr? = null
)