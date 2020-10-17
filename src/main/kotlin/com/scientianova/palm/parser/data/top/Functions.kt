package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.TypeConstraints
import com.scientianova.palm.util.PString

enum class InlineHandling {
    None, NoInline, CrossInline
}

sealed class ParamModifier {
    object CrossInline : ParamModifier()
    object NoInline : ParamModifier()
    object Using : ParamModifier()
    data class Annotation(val annotation: com.scientianova.palm.parser.data.top.Annotation) : ParamModifier()
}

data class Function(
    val name: PString,
    val modifiers: List<DecModifier>,
    val typeParams: List<PString>,
    val constraints: TypeConstraints,
    val params: List<FunParam>,
    val type: PType?,
    val expr: PExpr?
)

data class FunParam(
    val modifiers: List<ParamModifier>,
    val pattern: PDecPattern,
    val type: PType,
    val default: PExpr? = null
)

data class OptionallyTypedFunParam(
    val modifiers: List<ParamModifier>,
    val pattern: PDecPattern,
    val type: PType?
)