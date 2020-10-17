package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.parser.data.top.Annotation
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

typealias Path = List<PString>

sealed class Type {
    data class Named(val path: Path, val generics: List<Positioned<TypeArg>>) : Type()
    data class Function(val params: List<FunTypeArg>, val returnType: PType) : Type()
    data class Nullable(val type: PType) : Type()
    data class Intersection(val types: List<PType>) : Type()
    data class Annotated(val annotation: Annotation, val type: PType) : Type()
}

typealias PType = Positioned<Type>

enum class VarianceMod {
    None, In, Out
}

sealed class TypeArg {
    data class Normal(val type: PType, val variance: VarianceMod) : TypeArg()
    object Wildcard : TypeArg()
}

typealias PTypeArg = Positioned<TypeArg>

data class FunTypeArg(val type: PType, val using: Boolean)