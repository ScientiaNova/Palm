package com.scientianova.palm.parser.data.top

import com.scientianova.palm.parser.data.expressions.Arg
import com.scientianova.palm.parser.data.expressions.Path

data class Annotation(val path: Path, val args: List<Arg>, val type: AnnotationType)

enum class AnnotationType {
    Normal, Get, Set, File, Field, Delegate, Property, Param, SetParam
}