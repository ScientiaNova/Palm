package com.scientianova.palm.tokenizer

data class PrefixOperatorToken internal constructor(val symbol: String) : IToken
data class PostfixOperatorToken internal constructor(val symbol: String) : IToken
data class InfixOperatorToken internal constructor(val symbol: String) : IToken