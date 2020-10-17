package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Arg
import com.scientianova.palm.parser.data.expressions.Path

sealed class DecModifier {
    object Public : DecModifier()
    object Protected : DecModifier()
    object Internal : DecModifier()
    object Private : DecModifier()
    object Lateinit : DecModifier()
    object Inline : DecModifier()
    object Tailrec : DecModifier()
    object Ann : DecModifier()
    object Abstract : DecModifier()
    object Leaf : DecModifier()
    object Partial : DecModifier()
    object Static : DecModifier()
    object Override : DecModifier()
    object Operator : DecModifier()
    object Blank : DecModifier()
    object In : DecModifier()
    object Out : DecModifier()
    object NoInline : DecModifier()
    object CrossInline : DecModifier()
    object Using : DecModifier()
    data class Annotation(val annotation: com.scientianova.palm.parser.data.top.Annotation) : DecModifier()
}

data class Annotation(val path: Path, val args: List<Arg>, val type: AnnotationType)

enum class AnnotationType {
    Normal, Get, Set, File, Field, Delegate, Property, Param, SetParam, Constructor
}