package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.expressions.PDecPattern
import com.palmlang.palm.ast.expressions.PExpr
import com.palmlang.palm.ast.expressions.PType

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