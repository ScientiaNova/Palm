package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.types.PType
import com.scientianova.palm.parser.data.types.PTypeParam
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