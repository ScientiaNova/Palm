package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Type
typealias PType = Positioned<Type>

typealias Path = List<PString>

data class NamedType(
    val path: Path,
    val generics: List<PType> = emptyList()
) : Type()

data class FunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()