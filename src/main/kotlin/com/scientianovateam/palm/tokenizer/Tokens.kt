package com.scientianovateam.palm.tokenizer

import com.scientianovateam.palm.Positioned

interface IToken {
    override fun toString(): String
}

typealias PositionedToken = Positioned<IToken>

sealed class OperatorToken(private val symbol: String, val precedence: Int) : IToken {
    override fun toString() = "Operator(symbol=$symbol)"
}

data class NumberToken(val number: Double) : IToken {
    constructor(number: Number) : this(number.toDouble())
}

sealed class BoolToken(val value: Boolean) : IToken {
    override fun toString() = "BoolToken(value=$value)"
}

object TrueToken : BoolToken(true)
object FalseToken : BoolToken(false)

object NullToken : IToken {
    override fun toString() = "NullToken"
}

data class CapitalizedIdentifierToken(val name: String) : IToken
data class UncapitalizedIdentifierToken(val name: String) : IToken

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object IfToken : KeywordToken("if")
object ThenToken : KeywordToken("then")
object ElseToken : KeywordToken("else")
object WhenToken : KeywordToken("when")
object WhereToken : KeywordToken("where")
object ForToken : KeywordToken("for")

object InToken : OperatorToken("in", 5)
object NotInToken : OperatorToken("!in", 5)
object IsToken : OperatorToken("is", 5)
object NotIsToken : OperatorToken("!is", 5)

private val specialWords = mapOf(
    "true" to TrueToken,
    "false" to FalseToken,
    "null" to NullToken,
    "if" to IfToken,
    "then" to ThenToken,
    "else" to ElseToken,
    "when" to WhenToken,
    "where" to WhereToken,
    "for" to ForToken,
    "in" to InToken,
    "is" to IsToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: UncapitalizedIdentifierToken(string)

sealed class SpecialSymbol(private val symbol: String) : IToken {
    override fun toString() = "SpecialSymbol(symbol=$symbol)"
}

object AssignmentToken : SpecialSymbol("=")
object OpenParenToken : SpecialSymbol("(")
object ClosedParenToken : SpecialSymbol(")")
object OpenCurlyBracketToken : SpecialSymbol("{")
object ClosedCurlyBracketToken : SpecialSymbol("}")
object OpenSquareBracketToken : SpecialSymbol("[")
object ClosedSquareBracketToken : SpecialSymbol("]")
object DotToken : SpecialSymbol(".")
object CommaToken : SpecialSymbol(",")
object ColonToken : SpecialSymbol(":")
object SemicolonToken : SpecialSymbol(";")
object DoubleDotToken : SpecialSymbol("..")
object SafeAccessToken : SpecialSymbol("?.")
object ArrowToken : SpecialSymbol("->")

object OrToken : OperatorToken("||", 1)
object AndToken : OperatorToken("&&", 2)
object EqualToken : OperatorToken("==", 3)
object NotEqualToken : OperatorToken("!=", 3)
object LessToken : OperatorToken("<", 4)
object LessOrEqualToken : OperatorToken("<=", 4)
object GreaterToken : OperatorToken(">", 4)
object GreaterOrEqualToken : OperatorToken("<=", 4)
object ElvisToken : OperatorToken("?:", 6)
object PlusToken : OperatorToken("+", 7)
object MinusToken : OperatorToken("-", 7)
object TimesToken : OperatorToken("*", 8)
object DivideToken : OperatorToken("/", 8)
object ModulusToken : OperatorToken("%", 8)
object FloorDivideToken : OperatorToken("//", 8)
object ExponentToken : OperatorToken("^", 9)
object NotToken : OperatorToken("!", 10)

private val symbolMap = mapOf(
    "=" to AssignmentToken,
    "." to DotToken,
    "," to CommaToken,
    ":" to ColonToken,
    ";" to SemicolonToken,
    ".." to DoubleDotToken,
    "?." to SafeAccessToken,
    "->" to ArrowToken,
    "||" to OrToken,
    "&&" to AndToken,
    "==" to EqualToken,
    "!=" to NotEqualToken,
    "<" to LessToken,
    "<=" to LessOrEqualToken,
    ">" to GreaterToken,
    ">=" to GreaterOrEqualToken,
    "?:" to ElvisToken,
    "+" to PlusToken,
    "-" to MinusToken,
    "*" to TimesToken,
    "/" to DivideToken,
    "%" to ModulusToken,
    "//" to FloorDivideToken,
    "^" to ExponentToken,
    "!" to NotToken
)

fun handleSymbol(symbol: String) = symbolMap[symbol] ?: error("Unsupported symbol $symbol")

data class StringToken(val parts: List<StringTokenPart>) : IToken

sealed class StringTokenPart
data class StringPart(val string: String) : StringTokenPart() {
    constructor(builder: StringBuilder) : this(builder.toString())
}

data class TokensPart(val tokens: TokenStack) : StringTokenPart() {
    constructor(token: PositionedToken) : this(TokenStack().apply { push(token) })
}

data class CharToken(val char: Char) : IToken