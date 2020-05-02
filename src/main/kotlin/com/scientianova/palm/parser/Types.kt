package com.scientianova.palm.parser

import com.scientianova.palm.util.Positioned

enum class Nullability {
    NON_NULL,
    NULLABLE,
    UNKNOWN,
}

data class Type(
    val name: String,
    val generics: List<TypeArg> = emptyList(),
    val nullability: Nullability = Nullability.NON_NULL
)

typealias PType = Positioned<Type>

enum class Variance {
    INVARIANT,
    COVARIANT,
    CONTRAVARIANT
}

sealed class TypeArg
object WildcardArgument : TypeArg()
data class GenericType(
    val name: String,
    val generics: List<TypeArg> = emptyList(),
    val nullability: Nullability = Nullability.NON_NULL,
    val variance: Variance
) : TypeArg()

typealias PTypeArg = Positioned<Type>

data class TypeVar(val name: String, val bounds: List<PType>)
typealias PTypeVar = Positioned<TypeVar>