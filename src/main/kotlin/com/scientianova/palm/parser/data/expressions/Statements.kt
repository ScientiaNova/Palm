package com.scientianova.palm.parser.data.expressions

sealed class ScopeStatement

data class DecStatement(
    val pattern: PDecPattern,
    val mutable: Boolean,
    val type: PType?,
    val expr: PExpr?
) : ScopeStatement()

data class AssignStatement(
    val left: PExpr,
    val type: AssignmentType,
    val right: PExpr
) : ScopeStatement()

data class GuardStatement(
    val cond: List<Condition>,
    val body: ExprScope
) : ScopeStatement()

data class DeferStatement(val body: ExprScope) : ScopeStatement()
data class UsingStatement(val expr: PExpr) : ScopeStatement()
data class ExprStatement(val expr: PExpr) : ScopeStatement()

typealias ExprScope = List<ScopeStatement>