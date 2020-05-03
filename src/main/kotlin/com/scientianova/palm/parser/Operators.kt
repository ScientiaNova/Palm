package com.scientianova.palm.parser

import com.scientianova.palm.util.PString

data class UnaryOpExpr(val symbol: PString, val expr: PExpression) : IExpression
data class PostfixOpExpr(val symbol: PString, val expr: PExpression) : IExpression

sealed class BinOp
data class SymbolOp(val symbol: PString) : BinOp()
data class InfixCall(val name: PString, val negated: Boolean) : BinOp()
sealed class SpecialOp : BinOp()
object InOp : SpecialOp()
object NotInOp : SpecialOp()
object IsOp : SpecialOp()
object IsNotOp : SpecialOp()
object AsOp : SpecialOp()
object SafeAsOp : SpecialOp()

data class BinaryOpsExpr(val first: PExpression, val rest: List<Pair<BinOp, PExpression>>) : IExpression