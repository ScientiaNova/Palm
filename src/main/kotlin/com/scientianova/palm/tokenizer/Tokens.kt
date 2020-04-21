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

sealed class AssignmentToken(symbol: String) : SpecialSymbol(symbol)
object EqualToToken : AssignmentToken("=")
object OrEqualToken : AssignmentToken("|=")
object XorEqualToken : AssignmentToken("^=")
object AndEqualToken : AssignmentToken("&=")
object ShrEqualToken : AssignmentToken("<<=")
object ShlEqualToken : AssignmentToken(">>=")
object UshrEqualToken : AssignmentToken(">>>=")
object PlusEqualToken : AssignmentToken("+=")
object MinusEqualToken : AssignmentToken("-=")
object TimesEqualToken : AssignmentToken("*=")
object DivEqualToken : AssignmentToken("/=")
object RemEqualToken : AssignmentToken("%=")
object ColonToken : AssignmentToken(":")

object OpenParenToken : SpecialSymbol("(")
object ClosedParenToken : SpecialSymbol(")")
object OpenCurlyBracketToken : SpecialSymbol("{")
object ClosedCurlyBracketToken : SpecialSymbol("}")
object OpenSquareBracketToken : SpecialSymbol("[")
object ClosedSquareBracketToken : SpecialSymbol("]")
object DotToken : SpecialSymbol(".")
object SafeAccessToken : SpecialSymbol("?.")
object RightArrowToken : SpecialSymbol("->")
object LeftArrowToken : SpecialSymbol("<-")
object DoubleColonToken : SpecialSymbol("::")
object SpreadToken : SpecialSymbol("*")
object PostQuestionMark : SpecialSymbol("?")

sealed class SeparatorToken(symbol: String) : SpecialSymbol(symbol)
object CommaToken : SeparatorToken(",")
object SemicolonToken : SeparatorToken(";")

val SYMBOL_MAP = mapOf(
    "=" to EqualToToken,
    "|=" to OrEqualToken,
    "^=" to XorEqualToken,
    "&=" to AndEqualToken,
    ">>=" to ShrEqualToken,
    "<<=" to ShlEqualToken,
    ">>>=" to UshrEqualToken,
    "+=" to PlusEqualToken,
    "-=" to MinusEqualToken,
    "*=" to TimesEqualToken,
    "/=" to DivEqualToken,
    "%=" to RemEqualToken,
    "." to DotToken,
    "," to CommaToken,
    ":" to ColonToken,
    ";" to SemicolonToken,
    ".." to DoubleDotToken,
    "?." to SafeAccessToken,
    "->" to RightArrowToken,
    "<-" to LeftArrowToken,
    "::" to DoubleColonToken
)