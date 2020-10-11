package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeConstraints
import com.scientianova.palm.parser.data.types.TypeParam
import com.scientianova.palm.util.PString

enum class InlineHandling {
    None, NoInline, CrossInline
}

data class ParamInfo(
    val using: Boolean,
    val inlineHandling: InlineHandling
)

data class Function(
    val name: PString,
    val typeParams: List<TypeParam>,
    val constraints: TypeConstraints,
    val params: List<FunParam>,
    val type: PType?,
    val expr: PExpr?
)

data class FunParam(
    val info: ParamInfo,
    val pattern: PDecPattern,
    val type: PType,
    val default: PExpr? = null
)