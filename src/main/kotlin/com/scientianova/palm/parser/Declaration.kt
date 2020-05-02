package com.scientianova.palm.parser

sealed class Getter
data class MultiGetter(val scope: SolidScope) : Getter()
data class SingleGetter(val expr: PExpression) : Getter()

data class Setter(val name: String, val scope: NamedScope)

data class PropertyDec(
    val name: String,
    val type: PType,
    val expr: PExpression?,
    val getter: Getter?,
    val setter: Setter?
) : IStatement

data class VarDec(
    val name: String,
    val type: PType,
    val expr: PExpression?
) : IStatement

data class SingleFunctionDec(
    val name: String,
    val params: List<Pair<String, PType>>,
    val genericPool: List<PTypeVar>,
    val returnType: Type?,
    val expr: PExpression
) : IStatement

data class MultiFunctionDec(
    val name: String,
    val params: List<Pair<String, PType>>,
    val genericPool: List<PTypeVar>,
    val returnType: Type,
    val scope: NamedScope
) : IStatement