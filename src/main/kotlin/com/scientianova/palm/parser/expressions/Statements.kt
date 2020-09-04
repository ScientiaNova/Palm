package com.scientianova.palm.parser.expressions

import com.scientianova.palm.parser.types.PType
import com.scientianova.palm.util.PString

sealed class ScopeStatement

data class DecStatement(
    val name: PString,
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
    val conditions: List<Condition>,
    val body: ExprScope
) : ScopeStatement()

data class UsingStatement(val expr: PExpr) : ScopeStatement()
data class ExprStatement(val expr: PExpr) : ScopeStatement()

data class ExprScope(val statements: List<ScopeStatement>)