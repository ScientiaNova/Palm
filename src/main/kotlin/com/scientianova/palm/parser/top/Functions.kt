package com.scientianova.palm.parser.top

import com.scientianova.palm.parser.expressions.PDecPattern
import com.scientianova.palm.parser.expressions.PExpr
import com.scientianova.palm.parser.types.PType
import com.scientianova.palm.parser.types.PTypeParam
import com.scientianova.palm.util.PString

enum class ParamModifier {
    Using, Given
}

data class Function(
    val name: PString,
    val typeParams: List<PTypeParam>,
    val params: List<FunParam>,
    val type: PType?,
    val expr: PExpr?
)

data class FunParam(
    val modifier: List<ParamModifier>,
    val pattern: PDecPattern,
    val type: PType,
    val default: PExpr? = null
)