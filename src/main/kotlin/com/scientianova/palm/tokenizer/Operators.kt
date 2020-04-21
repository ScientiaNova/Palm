package com.scientianova.palm.tokenizer

import com.scientianova.palm.parser.*

abstract class PrefixOperatorToken internal constructor(private val symbol: String, val name: String) : IToken {
    override fun toString() = "PrefixOperator(symbol=$symbol)"
}


object UnaryPlusToken : PrefixOperatorToken("+", "unaryPlus")
object UnaryMinusToken : PrefixOperatorToken("-", "unaryMinus")
object InvertToken : PrefixOperatorToken("~", "inv")
object NotToken : PrefixOperatorToken("!", "not")
object PreIncToken : PrefixOperatorToken("++", "inc")
object PreDecToken : PrefixOperatorToken("--", "dec")


val PREFIX_OPS_MAP = mapOf(
    "+" to UnaryPlusToken,
    "-" to UnaryMinusToken,
    "~" to InvertToken,
    "!" to NotToken,
    "++" to PreIncToken,
    "--" to PreDecToken,
    "*" to SpreadToken
)

abstract class PostfixOperatorToken internal constructor(private val symbol: String, val name: String) : IToken {
    override fun toString() = "PostfixOperator(symbol=$symbol)"
}

object PostIncToken : PostfixOperatorToken("++", "inc")
object PostDecToken : PostfixOperatorToken("--", "dec")

val POSTFIX_OPS_MAP = mapOf(
    "++" to PostIncToken,
    "--" to PostDecToken,
    "?" to PostQuestionMark
)

abstract class InfixOperatorToken internal constructor(private val symbol: String, val precedence: Int) : IToken {
    override fun toString() = "BinaryOperator(symbol=$symbol)"
    abstract fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression
}

object TernaryOperatorToken : InfixOperatorToken("?", 0) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) = error("Impossible")
}

object OrToken : InfixOperatorToken("||", 1) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Disjunction(first as IExpression, second as IExpression)
}

object AndToken : InfixOperatorToken("&&", 2) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Conjunction(first as IExpression, second as IExpression)
}

object BitWiseOrToken : InfixOperatorToken("|", 3) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "or", listOf(second as IExpression))
}

object BitWiseXorToken : InfixOperatorToken("^", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "xor", listOf(second as IExpression))
}

object BitWiseAndToken : InfixOperatorToken("&", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "and", listOf(second as IExpression))
}

object EqualToken : InfixOperatorToken("==", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        EqualityCheck(first as IExpression, second as IExpression)
}

object NotEqualToken : InfixOperatorToken("!=", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(EqualityCheck(first as IExpression, second as IExpression), "not")
}

object EqualRefToken : InfixOperatorToken("===", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        RefEqualityCheck(first as IExpression, second as IExpression)
}

object NotEqualRefToken : InfixOperatorToken("!==", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(RefEqualityCheck(first as IExpression, second as IExpression), "not")
}

sealed class ComparisonOperatorToken(
    symbol: String,
    precedence: Int,
    val comparisonType: ComparisonType
) : InfixOperatorToken(symbol, precedence) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(comparisonType, VirtualCall(first as IExpression, "compareTo", listOf(second as IExpression)))
}

object LessToken : ComparisonOperatorToken("<", 7, ComparisonType.L)
object LessOrEqualToken : ComparisonOperatorToken("<=", 7, ComparisonType.LE)
object GreaterToken : ComparisonOperatorToken(">", 7, ComparisonType.G)
object GreaterOrEqualToken : ComparisonOperatorToken("<=", 7, ComparisonType.GE)

object ComparisonToken : InfixOperatorToken("<=>", 7) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "compare_to", listOf(second as IExpression))
}

object LeftShiftToken : InfixOperatorToken("<<", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "shl", listOf(second as IExpression))
}

object RightShiftToken : InfixOperatorToken(">>", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "shr", listOf(second as IExpression))
}

object URightShiftToken : InfixOperatorToken(">>>", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "ushr", listOf(second as IExpression))
}

object ElvisToken : InfixOperatorToken("?:", 10) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Elvis(first as IExpression, second as IExpression)
}

object DoubleDotToken : InfixOperatorToken("..", 12) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "range_to", listOf(second as IExpression))
}

object PlusToken : InfixOperatorToken("+", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "plus", listOf(second as IExpression))
}

object MinusToken : InfixOperatorToken("-", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "minus", listOf(second as IExpression))
}

object TimesToken : InfixOperatorToken("*", 14) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "mul", listOf(second as IExpression))
}

object DivideToken : InfixOperatorToken("/", 14) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "div", listOf(second as IExpression))
}

object ModulusToken : InfixOperatorToken("%", 14) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "rem", listOf(second as IExpression))
}
val BINARY_OPS_MAP = mapOf(
    "||" to OrToken,
    "&&" to AndToken,
    "|" to BitWiseOrToken,
    "^" to BitWiseXorToken,
    "&" to BitWiseAndToken,
    "==" to EqualToken,
    "!=" to NotEqualToken,
    "===" to EqualRefToken,
    "!==" to NotEqualRefToken,
    "<" to LessToken,
    "<=" to LessOrEqualToken,
    ">" to GreaterToken,
    ">=" to GreaterOrEqualToken,
    "<<" to LeftShiftToken,
    ">>" to RightShiftToken,
    ">>>" to URightShiftToken,
    "?" to TernaryOperatorToken,
    "?:" to ElvisToken,
    "+" to PlusToken,
    "-" to MinusToken,
    "*" to TimesToken,
    "/" to DivideToken,
    "%" to ModulusToken,
    "<=>" to ComparisonToken
)