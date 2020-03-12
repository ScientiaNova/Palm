package com.scientianovateam.palm.tokenizer

import com.scientianovateam.palm.parser.*
import com.scientianovateam.palm.util.Positioned

interface IToken {
    override fun toString(): String
}

interface IKeyToken : IToken {
    val name: String
}

typealias PositionedToken = Positioned<IToken>

interface IUnaryOperatorToken : IToken

sealed class OperatorToken(private val symbol: String, val precedence: Int) : IToken {
    override fun toString() = "Operator(symbol=$symbol)"
    abstract fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression
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
object InToken : ContainingOperatorToken("in", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        BinaryOp(Contains, second as IExpression, first as IExpression)
}

object NotInToken : ContainingOperatorToken("!in", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        UnaryOp(Not, BinaryOp(Contains, second as IExpression, first as IExpression))
}

sealed class TypeOperatorToken(symbol: String, precedence: Int) : OperatorToken(symbol, precedence)
object IsToken : TypeOperatorToken("is", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        TypeCheck(first as IExpression, second as PalmType)
}

object IsNotToken : TypeOperatorToken("!is", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        UnaryOp(Not, TypeCheck(first as IExpression, second as PalmType))
}

object AsToken : TypeOperatorToken("as", 10) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        Cast(first as IExpression, second as PalmType)
}

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
sealed class SeparatorToken(symbol: String) : SpecialSymbol(symbol)
object CommaToken : SeparatorToken(",")
object SemicolonToken : SeparatorToken(";")
object DoubleDotToken : SpecialSymbol("..")
object QuestionMarkToken : SpecialSymbol("?")
object SafeAccessToken : SpecialSymbol("?.")
object ArrowToken : SpecialSymbol("->")

object OrToken : OperatorToken("||", 1) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Disjunction(first as IExpression, second as IExpression)
}

object AndToken : OperatorToken("&&", 2) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Conjunction(first as IExpression, second as IExpression)
}

object EqualToken : OperatorToken("==", 3) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        EqualityCheck(first as IExpression, second as IExpression)
}

object NotEqualToken : OperatorToken("!=", 3) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        UnaryOp(Not, EqualityCheck(first as IExpression, second as IExpression))
}

sealed class ComparisonOperatorToken(symbol: String, precedence: Int) : OperatorToken(symbol, precedence)
object LessToken : ComparisonOperatorToken("<", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(ComparisonType.L, BinaryOp(Compare, first as IExpression, second as IExpression))
}

object LessOrEqualToken : ComparisonOperatorToken("<=", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(ComparisonType.LE, BinaryOp(Compare, first as IExpression, second as IExpression))
}

object GreaterToken : ComparisonOperatorToken(">", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(ComparisonType.G, BinaryOp(Compare, first as IExpression, second as IExpression))
}

object GreaterOrEqualToken : ComparisonOperatorToken("<=", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(ComparisonType.GE, BinaryOp(Compare, first as IExpression, second as IExpression))
}

object ComparisonToken : OperatorToken("<=>", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Compare, first as IExpression, second as IExpression)
}

object ElvisToken : OperatorToken("?:", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Elvis(first as IExpression, second as IExpression)
}

object PlusToken : OperatorToken("+", 7), IUnaryOperatorToken {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Add, first as IExpression, second as IExpression)
}

object MinusToken : OperatorToken("-", 7), IUnaryOperatorToken {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Sub, first as IExpression, second as IExpression)
}

object TimesToken : OperatorToken("*", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Mul, first as IExpression, second as IExpression)
}

object DivideToken : OperatorToken("/", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Div, first as IExpression, second as IExpression)
}

object ModulusToken : OperatorToken("%", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Rem, first as IExpression, second as IExpression)
}

object FloorDivideToken : OperatorToken("//", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(FloorDiv, first as IExpression, second as IExpression)
}

object ExponentToken : OperatorToken("^", 9) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        BinaryOp(Pow, first as IExpression, second as IExpression)
}

object NotToken : IUnaryOperatorToken {
    override fun toString() = "NotToken"
}

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
    "!" to NotToken,
    "<=>" to ComparisonToken
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