package com.scientianova.palm.parser.data.expressions

sealed class Pattern {
    data class Expr(val expr: PExpr) : Pattern()
    data class Type(val type: PType) : Pattern()
}