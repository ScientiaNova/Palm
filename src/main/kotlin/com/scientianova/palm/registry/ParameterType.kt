package com.scientianova.palm.registry

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

sealed class ParameterType
data class GenericParameter(val name: String, val bounds: List<ParameterType>) : ParameterType()
data class RegularType(val clazz: Class<*>) : ParameterType()
data class ParameterizedType(val raw: ParameterType, val params: List<ParameterType>) : ParameterType()
data class ArrayType(val type: ParameterType) : ParameterType()
data class WildcardType(val upperBounds: List<ParameterType>, val lowerBounds: List<ParameterType>) : ParameterType()

fun handleJavaType(type: Type): ParameterType = when (type) {
    is Class<*> -> RegularType(type)
    is TypeVariable<*> -> GenericParameter(type.name, type.bounds.map(::handleJavaType))
    is java.lang.reflect.ParameterizedType -> ParameterizedType(
        handleJavaType(type.rawType), type.actualTypeArguments.map(::handleJavaType)
    )
    is GenericArrayType -> ArrayType(handleJavaType(type.genericComponentType))
    is java.lang.reflect.WildcardType -> WildcardType(
        type.upperBounds.map(::handleJavaType), type.lowerBounds.map(::handleJavaType)
    )
    else -> error("Wat!?")
}

fun same(list: List<*>) = list