package com.scientianova.palm.parser

import com.scientianova.palm.util.PString

enum class ParamModifier {
    Using, Given
}

sealed class FunDec<T> {
    abstract val name: PString
    abstract val params: List<FunParam>
    abstract val info: T
}

data class Function<T>(
    override val name: PString,
    override val params: List<FunParam>,
    override val info: T,
    val type: PType?,
    val expr: PExpr
) : FunDec<T>()

data class FunParam(
    val modifier: List<ParamModifier>,
    val name: PString,
    val type: PType,
    val default: PExpr? = null
)

data class FileFunctionInfo(
    val privacy: TopLevelPrivacy,
    val inline: Boolean,
    val tailRec: Boolean,
    val given: Boolean,
    val using: Boolean
)

data class MethodInfo(
    val privacy: ClassLevelPrivacy,
    val abstract: Boolean,
    val tailRec: Boolean,
    val given: Boolean,
    val using: Boolean
)