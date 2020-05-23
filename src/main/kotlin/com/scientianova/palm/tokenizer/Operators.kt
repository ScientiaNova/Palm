package com.scientianova.palm.tokenizer

open class PrefixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "PrefixOperator(symbol=$symbol)"
}

open class PostfixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "PostfixOperator(symbol=$symbol)"
}

open class InfixOperatorToken internal constructor(private val symbol: String) : IToken {
    override fun toString() = "BinaryOperator(symbol=$symbol)"
}