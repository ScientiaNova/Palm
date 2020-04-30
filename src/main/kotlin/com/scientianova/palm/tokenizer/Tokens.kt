package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.Positioned

interface IToken {
    override fun toString(): String
}

interface IKeyToken : IToken {
    val name: String
}

typealias PositionedToken = Positioned<IToken>

sealed class SpecialSymbol(private val symbol: String) : IToken {
    override fun toString() = "SpecialSymbol(symbol=$symbol)"
}

object EqualToToken : InfixOperatorToken("=")
object ColonToken : InfixOperatorToken(":")

object OpenParenToken : SpecialSymbol("(")
object ClosedParenToken : SpecialSymbol(")")
object OpenCurlyBracketToken : SpecialSymbol("{")
object ClosedCurlyBracketToken : SpecialSymbol("}")
object OpenSquareBracketToken : SpecialSymbol("[")
object ClosedSquareBracketToken : SpecialSymbol("]")
object OpenArrayBracketToken : SpecialSymbol("[|")
object ClosedArrayBracketToken : SpecialSymbol("|]")
object DotToken : SpecialSymbol(".")
object SafeAccessToken : SpecialSymbol("?.")
object ArrowToken : SpecialSymbol("->")
object DoubleColonToken : SpecialSymbol("::")
object SpreadToken : SpecialSymbol("*")
object PostQuestionMark : SpecialSymbol("?")

sealed class SeparatorToken(symbol: String) : SpecialSymbol(symbol)
object CommaToken : SeparatorToken(",")
object SemicolonToken : SeparatorToken(";")

val SYMBOL_MAP = mapOf(
    "=" to EqualToToken,
    "." to DotToken,
    ":" to ColonToken,
    "?." to SafeAccessToken,
    "->" to ArrowToken,
    "::" to DoubleColonToken
)