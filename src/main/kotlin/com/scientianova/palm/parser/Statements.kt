package com.scientianova.palm.parser

import com.scientianova.palm.util.PString

sealed class ScopeStatement

data class VarDecStatement(val name: PString, val mutable: Boolean, val type: PType?, val expr: PExpr?) :
    ScopeStatement()

data class ExprStatement(val expr: PExpr) : ScopeStatement()

data class ExprScope(val statements: List<ScopeStatement>)