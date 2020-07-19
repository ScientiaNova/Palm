package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

sealed class ScopeStatement
typealias PSStatement = Positioned<ScopeStatement>

data class VariableDecStatement(val name: PString, val type: PType?, val expr: PExpr) : ScopeStatement()
data class UsingStatement(val expr: PExpr) : ScopeStatement()
data class ExprStatement(val expr: Expression) : ScopeStatement()

data class ExpressionScope(val statements: List<PSStatement>)