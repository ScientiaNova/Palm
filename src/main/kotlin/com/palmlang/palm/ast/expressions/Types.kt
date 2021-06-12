package com.palmlang.palm.ast.expressions

import com.palmlang.palm.ast.top.Annotation
import com.palmlang.palm.util.Path
import com.palmlang.palm.util.Positioned

sealed class Type {
    object Infer : Type()
    data class Named(val path: Path, val generics: List<Arg<PNestedType>>) : Type()
    data class Lis(val type: PType) : Type()
    data class Dict(val key: PType, val value: PType) : Type()
    data class Tuple(val types: List<PType>) : Type()
    data class Function(val context: List<PType>, val params: List<PType>, val returnType: PType) : Type()
    data class Nullable(val type: PType) : Type()
    data class Annotated(val annotation: Positioned<Annotation>, val type: PType) : Type()
    data class Parenthesized(val nested: PType) : Type()
    data class Byte(val value: kotlin.Byte) : Type()
    data class Short(val value: kotlin.Short) : Type()
    data class Int(val value: kotlin.Int) : Type()
    data class Long(val value: kotlin.Long) : Type()
    data class Float(val value: kotlin.Float) : Type()
    data class Double(val value: kotlin.Double) : Type()
    data class Char(val value: kotlin.Char) : Type()
    data class Str(val parts: List<StringPartP>) : Type()
    data class Bool(val value: Boolean) : Type()
    object Null : Type()
}

val emptyType = Type.Named(emptyList(), emptyList())

typealias PType = Positioned<Type>

enum class VarianceMod {
    None, In, Out
}

typealias PNestedType = Positioned<NestedType>

sealed class NestedType {
    data class Normal(val type: PType, val variance: VarianceMod) : NestedType()
    object Star : NestedType()
}