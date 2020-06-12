package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.Positioned

sealed class Token {
    abstract override fun toString(): String
}
typealias PToken = Positioned<Token>

object WildcardToken : Token() {
    override fun toString() = "WildcardToken"
}

object WhenToken : Token() {
    override fun toString() = "WhenToken"
}

object ImportToken : Token() {
    override fun toString() = "ImportToken"
}

object LetToken : Token() {
    override fun toString() = "LetToken"
}

sealed class SpecialSymbol(private val symbol: String) : Token() {
    override fun toString() = "SpecialSymbol(symbol=$symbol)"
}

object EqualsToken : Token() {
    override fun toString() = "EqualsToken"
}

object ColonToken : Token() {
    override fun toString() = "ColonToken"
}

data class ByteToken(val value: Byte) : Token()
data class ShortToken(val value: Short) : Token()
data class IntToken(val value: Int) : Token()
data class LongToken(val value: Long) : Token()
data class FloatToken(val value: Float) : Token()
data class DoubleToken(val value: Double) : Token()
data class CharToken(val char: Char) : Token()
data class IdentifierToken(val name: String, val capitalized: Boolean = false) : Token()

data class PureStringToken(val text: String) : Token()
data class StringTemplateToken(val parts: List<PStringTokenPart>) : Token()

sealed class SymbolToken : Token() {
    abstract val symbol: String
}

data class PrefixOperatorToken internal constructor(override val symbol: String) : SymbolToken()
data class PostfixOperatorToken internal constructor(override val symbol: String) : SymbolToken()
data class InfixOperatorToken internal constructor(override val symbol: String) : SymbolToken()

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
    "=" to EqualsToken,
    "." to DotToken,
    ":" to ColonToken,
    "->" to RightArrowToken,
    "<-" to LeftArrowToken,
    "=>" to ThickArrowToken,
    "..." to TripleDotToken,
    ">:" to SuperTypeToken,
    "<" to SubTypeToken
)