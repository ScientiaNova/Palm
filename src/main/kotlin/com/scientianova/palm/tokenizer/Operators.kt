package com.scientianova.palm.tokenizer

import com.scientianova.palm.parser.*

open class PrefixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "PrefixOperator(symbol=$symbol)"
}

object NotToken : PrefixOperatorToken("!")
object PreIncToken : PrefixOperatorToken("++")
object PreDecToken : PrefixOperatorToken("--")


val PREFIX_OPS_MAP = mapOf(
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

open class InfixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "BinaryOperator(symbol=$symbol)"
}

object TernaryOperatorToken : InfixOperatorToken("?")

object OrToken : InfixOperatorToken("||")

object AndToken : InfixOperatorToken("&&")

object EqualToken : InfixOperatorToken("==")

object NotEqualToken : InfixOperatorToken("!=")

object EqualRefToken : InfixOperatorToken("===")

object NotEqualRefToken : InfixOperatorToken("!==")

sealed class ComparisonOperatorToken(
    symbol: String,
    val comparisonType: ComparisonType
) : InfixOperatorToken(symbol)

object LessToken : ComparisonOperatorToken("<", ComparisonType.L)
object LessOrEqualToken : ComparisonOperatorToken("<=", ComparisonType.LE)
object GreaterToken : ComparisonOperatorToken(">", ComparisonType.G)
object GreaterOrEqualToken : ComparisonOperatorToken("<=", ComparisonType.GE)

object ElvisToken : InfixOperatorToken("?:")

object DoubleDotToken : InfixOperatorToken("..")

val BINARY_OPS_MAP = mapOf(
    "||" to OrToken,
    "&&" to AndToken,
    "==" to EqualToken,
    "!=" to NotEqualToken,
    "===" to EqualRefToken,
    "!==" to NotEqualRefToken,
    "<" to LessToken,
    "<=" to LessOrEqualToken,
    ">" to GreaterToken,
    ">=" to GreaterOrEqualToken,
    "?" to TernaryOperatorToken,
    "?:" to ElvisToken
)