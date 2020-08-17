package com.scientianova.palm.parser

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.at

sealed class ScopeStatement

data class VarDecStatement(val name: PString, val mutable: Boolean, val type: PType?, val expr: PExpr?) : ScopeStatement()
data class ExprStatement(val expr: PExpr) : ScopeStatement()

data class ExprScope(val statements: List<ScopeStatement>)
typealias PExprScope = Positioned<ExprScope>

fun PExprScope.toExpr() = ScopeExpr(value) at area
fun ParseResult<PExprScope>.toExpr() = map(PExprScope::toExpr)