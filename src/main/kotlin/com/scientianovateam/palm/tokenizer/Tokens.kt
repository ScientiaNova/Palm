package com.scientianovateam.palm.tokenizer

import com.scientianovateam.palm.util.Positioned

interface IToken {
    override fun toString(): String
}

interface IKeyToken : IToken {
    val name: String
}

typealias PositionedToken = Positioned<IToken>

sealed class OperatorToken(private val symbol: String, val precedence: Int) : IToken {
    override fun toString() = "Operator(symbol=$symbol)"
}

data class NumberToken(val number: Double) : IToken {
    constructor(number: Number) : this(number.toDouble())
}

sealed class BoolToken(val bool: Boolean) : IToken {
    override fun toString() = "BoolToken(value=$bool)"
}

object TrueToken : BoolToken(true)
object FalseToken : BoolToken(false)

object NullToken : IToken {
    override fun toString() = "NullToken"
}

data class CapitalizedIdentifierToken(val name: String) : IToken
data class UncapitalizedIdentifierToken(override val name: String) : IKeyToken

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object IfToken : KeywordToken("if")
object ThenToken : KeywordToken("then")
object ElseToken : KeywordToken("else")
object WhenToken : KeywordToken("when")
object WhereToken : KeywordToken("where")
object ForToken : KeywordToken("for")

sealed class ContainingOperatorToken(symbol: String, precedence: Int) : OperatorToken(symbol, precedence)
object InToken : ContainingOperatorToken("in", 5)
object NotInToken : ContainingOperatorToken("!in", 5)
sealed class TypeOperatorToken(symbol: String, precedence: Int) : OperatorToken(symbol, precedence)
object IsToken : TypeOperatorToken("is", 5)
object IsNotToken : TypeOperatorToken("!is", 5)
object AsToken : OperatorToken("as", 10)

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
    "is" to IsToken,
    "as" to AsToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: UncapitalizedIdentifierToken(string)

sealed class SpecialSymbol(private val symbol: String) : IToken {
    override fun toString() = "SpecialSymbol(symbol=$symbol)"
}

sealed class AssignmentToken(symbol: String) : SpecialSymbol(symbol)
object IsEqualToToken : AssignmentToken("=")
object ColonToken : AssignmentToken(":")
object OpenParenToken : SpecialSymbol("(")
object ClosedParenToken : SpecialSymbol(")")
object OpenCurlyBracketToken : SpecialSymbol("{")
object ClosedCurlyBracketToken : SpecialSymbol("}")
object OpenSquareBracketToken : SpecialSymbol("[")
object ClosedSquareBracketToken : SpecialSymbol("]")
object DotToken : SpecialSymbol(".")
object CommaToken : SpecialSymbol(",")
object SemicolonToken : SpecialSymbol(";")
object DoubleDotToken : SpecialSymbol("..")
object QuestionMarkToken : SpecialSymbol("?")
object SafeAccessToken : SpecialSymbol("?.")
object ArrowToken : SpecialSymbol("->")

object OrToken : OperatorToken("||", 1)
object AndToken : OperatorToken("&&", 2)
object EqualToken : OperatorToken("==", 3)
object NotEqualToken : OperatorToken("!=", 3)
sealed class ComparisonOperatorToken(symbol: String, precedence: Int) : OperatorToken(symbol, precedence)
object LessToken : ComparisonOperatorToken("<", 4)
object LessOrEqualToken : ComparisonOperatorToken("<=", 4)
object GreaterToken : ComparisonOperatorToken(">", 4)
object GreaterOrEqualToken : ComparisonOperatorToken("<=", 4)
object ElvisToken : OperatorToken("?:", 6)
object PlusToken : OperatorToken("+", 7)
object MinusToken : OperatorToken("-", 7)
object TimesToken : OperatorToken("*", 8)
object DivideToken : OperatorToken("/", 8)
object ModulusToken : OperatorToken("%", 8)
object FloorDivideToken : OperatorToken("//", 8)
object ExponentToken : OperatorToken("^", 9)
object NotToken : OperatorToken("!", 11)

private val symbolMap = mapOf(
    "=" to IsEqualToToken,
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
    "?" to QuestionMarkToken,
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

sealed class StringToken : IToken
data class PureStringToken(override val name: String) : StringToken(), IKeyToken
data class StringTemplateToken(val parts: List<StringTokenPart>) : StringToken()

sealed class StringTokenPart
data class StringPart(val string: String) : StringTokenPart() {
    constructor(builder: StringBuilder) : this(builder.toString())
}

data class TokensPart(val tokens: TokenStack) : StringTokenPart() {
    constructor(token: PositionedToken) : this(TokenStack().apply { push(token) })
}

data class CharToken(val char: Char) : IToken