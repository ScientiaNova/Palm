package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.at

sealed class ScopeStatement
typealias PSStatement = Positioned<ScopeStatement>

data class VariableDecStatement(val name: PString, val type: PType?, val expr: PExpr) : ScopeStatement()
data class UsingStatement(val expr: PExpr) : ScopeStatement()
data class ExprStatement(val expr: Expression) : ScopeStatement()

data class ExprScope(val statements: List<PSStatement>)
typealias PExprScope = Positioned<ExprScope>

fun PExprScope.toExpr() = ScopeExpr(value) at area