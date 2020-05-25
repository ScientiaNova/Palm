package com.scientianova.palm.tokenizer

sealed class SymbolToken : IToken {
    abstract val symbol: String
}
data class PrefixOperatorToken internal constructor(override val symbol: String) : SymbolToken()
data class PostfixOperatorToken internal constructor(override val symbol: String) : SymbolToken()
data class InfixOperatorToken internal constructor(override val symbol: String) : SymbolToken()