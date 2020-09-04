package com.scientianova.palm.parser.types

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

typealias Path = List<PString>

sealed class Type {
    data class Named(val path: Path, val generics: List<Positioned<TypeArg>>) : Type()
    data class Function(val params: List<FunTypeArg>, val returnType: PType) : Type()
    data class Nullable(val type: PType) : Type()
    data class Intersection(val types: List<PType>) : Type()
}

typealias PType = Positioned<Type>

enum class VarianceMod {
    None, In, Out
}

data class TypeParam(val name: PString, val lowerBounds: List<PType>, val variance: VarianceMod)
typealias PTypeParam = Positioned<TypeParam>

data class TypeArg(val type: PType, val variance: VarianceMod)
typealias PTypeArg = Positioned<TypeArg>

data class FunTypeArg(val type: PType, val using: Boolean)