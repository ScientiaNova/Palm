package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.expressions.Arg
import com.palmlang.palm.ast.expressions.PExpr
import com.palmlang.palm.util.Path
import com.palmlang.palm.util.PathType
import com.palmlang.palm.util.Positioned

sealed class DecModifier {
    object Public : DecModifier()
    object Protected : DecModifier()
    object Local : DecModifier()
    object Inline : DecModifier()
    object Ann : DecModifier()
    object Abstract : DecModifier()
    object Override : DecModifier()
    object Open : DecModifier()
    object Final : DecModifier()
    object Const : DecModifier()
    object Leaf : DecModifier()
    object Data : DecModifier()
    object NoInline : DecModifier()
    object CrossInline : DecModifier()
    data class Private(val pathType: PathType?, val path: Path) : DecModifier()
    data class Sealed(val pathType: PathType?, val path: Path) : DecModifier()
    data class Annotation(val annotation: com.palmlang.palm.ast.top.Annotation) : DecModifier()
}

typealias PDecMod = Positioned<DecModifier>

data class Annotation(val path: Path, val args: List<Arg<PExpr>>, val type: AnnotationType)

enum class AnnotationType {
    Normal, Get, Set, File, Field, Delegate, Property, Param, SetParam, Init
}