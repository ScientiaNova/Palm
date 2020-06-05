package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.Positioned

interface IToken {
    override fun toString(): String
}

typealias PToken = Positioned<IToken>

sealed class SpecialSymbol(private val symbol: String) : IToken {
    override fun toString() = "SpecialSymbol(symbol=$symbol)"
}

object EqualToToken : IToken {
    override fun toString() = "EqualToToken"
}
object ColonToken : IToken {
    override fun toString() = "ColonToken"
}

object OpenParenToken : SpecialSymbol("(")
object ClosedParenToken : SpecialSymbol(")")
object OpenCurlyBracketToken : SpecialSymbol("{")
object ClosedCurlyBracketToken : SpecialSymbol("}")
object OpenSquareBracketToken : SpecialSymbol("[")
object ClosedSquareBracketToken : SpecialSymbol("]")
object OpenArrayBracketToken : SpecialSymbol("[|")
object ClosedArrayBracketToken : SpecialSymbol("|]")
object DotToken : SpecialSymbol(".")
object RightArrowToken : SpecialSymbol("->")
object LeftArrowToken : SpecialSymbol("<-")
object ThickArrowToken : SpecialSymbol("=>")
object SuperTypeToken : SpecialSymbol(">:")
object SubTypeToken : SpecialSymbol("<:")
object TripleDotToken : SpecialSymbol("...")

sealed class SeparatorToken(symbol: String) : SpecialSymbol(symbol)
object CommaToken : SeparatorToken(",")
object SemicolonToken : SeparatorToken(";")

val SYMBOL_MAP = mapOf(
    "=" to EqualToToken,
    "." to DotToken,
    ":" to ColonToken,
    "->" to RightArrowToken,
    "<-" to LeftArrowToken,
    "=>" to ThickArrowToken,
    "..." to TripleDotToken,
    ">:" to SuperTypeToken,
    "<" to SubTypeToken
)