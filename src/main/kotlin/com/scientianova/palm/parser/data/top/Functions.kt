package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeConstraints
import com.scientianova.palm.util.PString

data class Function(
    val name: PString,
    val modifiers: List<PDecMod>,
    val typeParams: List<PString>,
    val constraints: TypeConstraints,
    val context: List<ContextParam>,
    val params: List<FunParam>,
    val type: PType?,
    val expr: PExpr?
)

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