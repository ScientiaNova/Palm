package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeConstraints
import com.scientianova.palm.util.PString

data class FunParam(
    val modifiers: List<PDecMod>,
    val pattern: PDecPattern,
    val type: PType,
    val default: PExpr?
)

data class OptionallyTypedFunParam(
    val modifiers: List<PDecMod>,
    val pattern: PDecPattern,
    val type: PType?,
    val default: PExpr?
)

data class ContextParam(
    val modifiers: List<PDecMod>,
    val pattern: PDecPattern,
    val type: PType
)