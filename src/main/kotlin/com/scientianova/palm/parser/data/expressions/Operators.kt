package com.scientianova.palm.parser.data.expressions

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.parsing.expressions.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

typealias PBinOp = Positioned<BinOp>

enum class UnOp {
    Not, Plus, Minus, NonNull
}

sealed class BinOp(val precedence: Int) {
    abstract fun handle(start: StringPos, next: StringPos, left: PExpr, parser: Parser): Pair<PExpr, PBinOp?>
}

sealed class TypeOp(precedence: Int) : BinOp(precedence) {
    abstract fun toExpr(left: PExpr, right: PType): Expr

    override fun handle(start: StringPos, next: StringPos, left: PExpr, parser: Parser): Pair<PExpr, PBinOp?> {
        val type = parser.requireType()
        return toExpr(left, type).at(left.start, type.next) to parser.parseOp()
    }
}

sealed class ExprOp(precedence: Int) : BinOp(precedence) {
    override fun handle(start: StringPos, next: StringPos, left: PExpr, parser: Parser): Pair<PExpr, PBinOp?> {
        val currRight = parser.requireSubExpr()
        val res = parser.parseOp()?.let { op ->
            parser.parseBinOps(currRight, op, precedence)
        } ?: run {
            currRight to null
        }
        val right = res.first
        return Expr.Binary(this.at(start, next), left, right).at(left.start, right.next) to res.second
    }
}

data class Not(val op: PBinOp) : BinOp(op.value.precedence) {
    override fun handle(start: StringPos, next: StringPos, left: PExpr, parser: Parser): Pair<PExpr, PBinOp?> {
        val res = op.value.handle(op.start, op.next, left, parser)
        return Expr.Unary(UnOp.Not.at(start), res.first).at(left.start, res.first.next) to res.second
    }
}


object As : TypeOp(11) {
    override fun toExpr(left: PExpr, right: PType) = Expr.TypeInfo(left, right)
}

object NullableAs : TypeOp(11) {
    override fun toExpr(left: PExpr, right: PType) = Expr.NullableCast(left, right)
}

object UnsafeAs : TypeOp(11) {
    override fun toExpr(left: PExpr, right: PType) = Expr.UnsafeCast(left, right)
}


object Times : ExprOp(10)
object Div : ExprOp(10)
object Rem : ExprOp(10)


object Plus : ExprOp(9)
object Minus : ExprOp(9)


object RangeTo : ExprOp(8)


data class Infix(val name: String) : ExprOp(7)


object Elvis : ExprOp(6)

object Is : BinOp(5) {
    override fun handle(start: StringPos, next: StringPos, left: PExpr, parser: Parser): Pair<PExpr, PBinOp?> {
        val type = parser.requireType()
        val destructuring = parser.parseDestructuring()
        return Expr.TypeCheck(left, type, destructuring).at(left.start, type.next) to parser.parseOp()
    }
}


object Less : ExprOp(4)
object Greater : ExprOp(4)
object LessOrEq : ExprOp(4)
object GreaterOrEq : ExprOp(4)


object Eq : ExprOp(3)

object RefEq : ExprOp(3)


object And : ExprOp(2)


object Or : ExprOp(1)


object Assign : ExprOp(0)
object PlusAssign : ExprOp(0)
object MinusAssign : ExprOp(0)
object TimesAssign : ExprOp(0)
object DivAssign : ExprOp(0)
object RemAssign : ExprOp(0)