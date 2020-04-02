package com.scientianova.palm.tokenizer

import com.scientianova.palm.parser.*
import com.scientianova.palm.util.Positioned

interface IToken {
    override fun toString(): String
}

interface IKeyToken : IToken {
    val name: String
}

typealias PositionedToken = Positioned<IToken>

sealed class UnaryOperatorToken(private val symbol: String, val name: String) : IToken {
    override fun toString() = "UnaryOperator(symbol=$symbol)"
}

sealed class BinaryOperatorToken(private val symbol: String, val precedence: Int) : IToken {
    override fun toString() = "BinaryOperator(symbol=$symbol)"
    abstract fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression
}

data class NumberToken(val number: Number) : IToken

sealed class BoolToken(val bool: Boolean) : IToken {
    override fun toString() = "BoolToken(value=$bool)"
}

object TrueToken : BoolToken(true)
object FalseToken : BoolToken(false)

object NullToken : IToken {
    override fun toString() = "NullToken"
}

data class IdentifierToken(override val name: String) : BinaryOperatorToken(name, 10), IKeyToken {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, name, listOf(second as IExpression))
}

sealed class KeywordToken(private val word: String) : IToken {
    override fun toString() = "KeywordToken(word=$word)"
}

object IfToken : KeywordToken("if")
object ThenToken : KeywordToken("then")
object ElseToken : KeywordToken("else")
object WhenToken : KeywordToken("when")
object WhereToken : KeywordToken("where")
object ForToken : KeywordToken("for")
object YieldToken : KeywordToken("yield")

sealed class ContainingOperatorToken(symbol: String, precedence: Int) : BinaryOperatorToken(symbol, precedence)
object InToken : ContainingOperatorToken("in", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(second as IExpression, "contains", listOf(first as IExpression))
}

object NotInToken : ContainingOperatorToken("!in", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(VirtualCall(second as IExpression, "contains", listOf(first as IExpression)), "not")
}

sealed class TypeOperatorToken(symbol: String, precedence: Int) : BinaryOperatorToken(symbol, precedence)
object IsToken : TypeOperatorToken("is", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        TypeCheck(first as IExpression, (second as PalmType).path)
}

object IsNotToken : TypeOperatorToken("!is", 8) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        VirtualCall(TypeCheck(first as IExpression, (second as PalmType).path), "not")
}

object AsToken : TypeOperatorToken("as", 15) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart): IExpression =
        Cast(first as IExpression, (second as PalmType).path)
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
    "as" to AsToken,
    "yield" to YieldToken
)

fun handleUncapitalizedString(string: String) = specialWords[string] ?: IdentifierToken(string)

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
object SafeAccessToken : SpecialSymbol("?.")
object ArrowToken : SpecialSymbol("->")
object DoubleColonToken : SpecialSymbol("::")

object TernaryOperatorToken : BinaryOperatorToken("?", 0) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) = error("Impossible")
}

object OrToken : BinaryOperatorToken("||", 1) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Disjunction(first as IExpression, second as IExpression)
}

object AndToken : BinaryOperatorToken("&&", 2) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Conjunction(first as IExpression, second as IExpression)
}

object BitWiseOrToken : BinaryOperatorToken("|", 3) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "or", listOf(second as IExpression))
}

object BitWiseAndToken : BinaryOperatorToken("&", 4) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "and", listOf(second as IExpression))
}

object EqualToken : BinaryOperatorToken("==", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        EqualityCheck(first as IExpression, second as IExpression)
}

object NotEqualToken : BinaryOperatorToken("!=", 5) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(EqualityCheck(first as IExpression, second as IExpression), "not")
}

sealed class ComparisonOperatorToken(
    symbol: String,
    precedence: Int,
    val comparisonType: ComparisonType
) : BinaryOperatorToken(symbol, precedence) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Comparison(comparisonType, VirtualCall(first as IExpression, "compare_to", listOf(second as IExpression)))
}

object LessToken : ComparisonOperatorToken("<", 6, ComparisonType.L)
object LessOrEqualToken : ComparisonOperatorToken("<=", 6, ComparisonType.LE)
object GreaterToken : ComparisonOperatorToken(">", 6, ComparisonType.G)
object GreaterOrEqualToken : ComparisonOperatorToken("<=", 6, ComparisonType.GE)

object ComparisonToken : BinaryOperatorToken("<=>", 6) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "compare_to", listOf(second as IExpression))
}

object LeftShiftToken : BinaryOperatorToken("<<", 7) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "shl", listOf(second as IExpression))
}

object RightShiftToken : BinaryOperatorToken(">>", 7) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "shr", listOf(second as IExpression))
}

object URightShiftToken : BinaryOperatorToken(">>>", 7) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "ushr", listOf(second as IExpression))
}

object ElvisToken : BinaryOperatorToken("?:", 9) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        Elvis(first as IExpression, second as IExpression)
}

object DoubleDotToken : BinaryOperatorToken("..", 11) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "range_to", listOf(second as IExpression))
}

object PlusToken : BinaryOperatorToken("+", 12) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "plus", listOf(second as IExpression))
}

object MinusToken : BinaryOperatorToken("-", 12) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "minus", listOf(second as IExpression))
}

object TimesToken : BinaryOperatorToken("*", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "mul", listOf(second as IExpression))
}

object DivideToken : BinaryOperatorToken("/", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "div", listOf(second as IExpression))
}

object ModulusToken : BinaryOperatorToken("%", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "rem", listOf(second as IExpression))
}

object FloorDivideToken : BinaryOperatorToken("//", 13) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "floor_div", listOf(second as IExpression))
}

object ExponentToken : BinaryOperatorToken("^", 14) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) =
        VirtualCall(first as IExpression, "pow", listOf(second as IExpression))
}

object WalrusOperatorToken : BinaryOperatorToken(":=", 16) {
    override fun handleExpression(first: IOperationPart, second: IOperationPart) = error("Impossible")
}

object UnaryPlusToken : UnaryOperatorToken("+", "unary_plus")
object UnaryMinusToken : UnaryOperatorToken("-", "unary_minus")
object InvertToken : UnaryOperatorToken("~", "inv")
object NotToken : UnaryOperatorToken("!", "not")

val SYMBOL_MAP = mapOf(
    "=" to IsEqualToToken,
    "." to DotToken,
    "," to CommaToken,
    ":" to ColonToken,
    ";" to SemicolonToken,
    ".." to DoubleDotToken,
    "?." to SafeAccessToken,
    "->" to ArrowToken,
    "::" to DoubleColonToken
)

val UNARY_OPS_MAP = mapOf(
    "+" to UnaryPlusToken,
    "-" to UnaryMinusToken,
    "~" to InvertToken,
    "!" to NotToken
)

val BINARY_OPS_MAP = mapOf(
    "||" to OrToken,
    "&&" to AndToken,
    "|" to BitWiseOrToken,
    "&" to BitWiseAndToken,
    "==" to EqualToken,
    "!=" to NotEqualToken,
    "<" to LessToken,
    "<=" to LessOrEqualToken,
    ">" to GreaterToken,
    ">=" to GreaterOrEqualToken,
    "<<" to LeftShiftToken,
    ">>" to RightShiftToken,
    ">>>" to URightShiftToken,
    ":=" to WalrusOperatorToken,
    "?" to TernaryOperatorToken,
    "?:" to ElvisToken,
    "+" to PlusToken,
    "-" to MinusToken,
    "*" to TimesToken,
    "/" to DivideToken,
    "%" to ModulusToken,
    "//" to FloorDivideToken,
    "^" to ExponentToken,
    "<=>" to ComparisonToken
)

sealed class StringToken : IToken
data class PureStringToken(override val name: String) : StringToken(), IKeyToken
data class StringTemplateToken(val parts: List<StringTokenPart>) : StringToken()

sealed class StringTokenPart
data class StringPart(val string: String) : StringTokenPart() {
    constructor(builder: StringBuilder) : this(builder.toString())
}

data class TokensPart(val tokens: TokenList) : StringTokenPart() {
    constructor(token: PositionedToken) : this(TokenList().apply { offer(token) })
}

data class CharToken(val char: Char) : IToken