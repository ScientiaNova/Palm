package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Arg
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.expressions.Path

sealed class DecModifier {
    object Public : DecModifier()
    object Protected : DecModifier()
    object Internal : DecModifier()
    object Private : DecModifier()
    object Lateinit : DecModifier()
    object Inline : DecModifier()
    object Ann : DecModifier()
    object Abstract : DecModifier()
    object Override : DecModifier()
    object Operator : DecModifier()
    object Open : DecModifier()
    object Final : DecModifier()
    object Const : DecModifier()
    object Enum : DecModifier()
    object Sealed : DecModifier()
    object Data : DecModifier()
    object NoInline : DecModifier()
    object CrossInline : DecModifier()
    data class Annotation(val annotation: com.scientianova.palm.parser.data.top.Annotation) : DecModifier()
}

data class Annotation(val path: Path, val args: List<Arg<PExpr>>, val type: AnnotationType)

enum class AnnotationType {
    Normal, Get, Set, File, Field, Delegate, Property, Param, SetParam, Init
}