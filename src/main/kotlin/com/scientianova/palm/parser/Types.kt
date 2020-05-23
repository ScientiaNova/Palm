package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class Type
typealias PType = Positioned<Type>

data class NamedType(
    val path: List<PString>,
    val generics: List<PType> = emptyList()
) : Type()

data class TupleType(
    val types: List<PType> = emptyList()
) : Type()

data class FunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()

object UnitType : Type()

sealed class TypeReq
typealias PTypeReq = Positioned<TypeReq>

data class SuperTypeReq(val type: PString, val superType: PType)
data class SubTypeReq(val type: PString, val subType: PType)
data class TypeClassReq(val typeClass: PType)