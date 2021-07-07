package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.ASTNode
import com.palmlang.palm.ast.top.Annotation
import com.palmlang.palm.util.Path
import com.palmlang.palm.util.Positioned

sealed interface Type : ASTNode {
    data class Named(val path: Path, val generics: List<Arg<PNestedType>>) : Type
    data class Lis(val type: PType) : Type
    data class Dict(val key: PType, val value: PType) : Type
    data class Tuple(val types: List<PType>) : Type
    data class Function(val context: List<PType>, val params: List<PType>, val returnType: PType) : Type
    data class Nullable(val type: PType) : Type
    data class Annotated(val annotation: Positioned<Annotation>, val type: PType) : Type
    data class Parenthesized(val nested: PType) : Type
}

val emptyType = Type.Named(emptyList(), emptyList())

typealias PType = Positioned<Type>

enum class VarianceMod {
    None, In, Out
}

typealias PNestedType = Positioned<NestedType>

sealed interface NestedType {
    data class Normal(val type: PType, val variance: VarianceMod) : NestedType
    object Star : NestedType
}