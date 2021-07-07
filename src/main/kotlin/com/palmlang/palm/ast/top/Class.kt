package com.palmlang.palm.ast.top

import com.palmlang.palm.ast.expressions.Arg
import com.palmlang.palm.ast.expressions.PExpr
import com.palmlang.palm.ast.expressions.PType
import com.palmlang.palm.ast.expressions.VarianceMod
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned

enum class DecHandling {
    None, Umm, Mut
}

sealed interface SuperType {
    data class Class(val type: PType, val args: List<Arg<PExpr>>) : SuperType
    data class Interface(val type: PType) : SuperType
}

typealias PSuperType = Positioned<SuperType>

data class PrimaryParam(
    val modifiers: List<PDecMod>,
    val decHandling: DecHandling,
    val name: PString,
    val type: PType,
    val default: PExpr?
)

data class TypeParam(val type: PString, val variance: VarianceMod, val lowerBound: List<PType>)
typealias PTypeParam = Positioned<TypeParam>

typealias WhereClause = List<Pair<PString, List<PType>>>