package com.scientianova.palm.tokenizer

open class PrefixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "PrefixOperator(symbol=$symbol)"
}

object NotToken : PrefixOperatorToken("!")


val PREFIX_OPS_MAP = mapOf(
    "!" to NotToken,
    "*" to SpreadToken
)

open class PostfixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "PostfixOperator(symbol=$symbol)"
}

val POSTFIX_OPS_MAP = mapOf(
    "?" to PostQuestionMark
)

open class InfixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "BinaryOperator(symbol=$symbol)"
}