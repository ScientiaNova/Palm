package com.scientianova.palm.lexer

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.StringPartP
import com.scientianova.palm.parser.parsing.expressions.parseStatements
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class StringPartL {
    abstract fun parse(parser: Parser): StringPartP
    data class String(val string: kotlin.String) : StringPartL() {
        override fun parse(parser: Parser) = StringPartP.String(string)
    }

    data class Expr(val startPos: StringPos, val tokens: List<PToken>) : StringPartL() {
        override fun parse(parser: Parser): StringPartP = StringPartP.Expr(
            com.scientianova.palm.parser.data.expressions.Expr.Scope(parser.scopedOf(tokens).parseStatements())
                .at(startPos, tokens.last().next)
        )
    }
}

sealed class Token {
    open fun canIgnore() = false
    open fun afterPostfix() = canIgnore()
    open fun beforePrefix() = canIgnore()

    data class Ident(val name: String, val backticked: Boolean) : Token()

    object NullLit : Token()
    data class BoolLit(val value: Boolean) : Token()
    data class CharLit(val value: Char) : Token()
    data class IntLit(val value: Long) : Token()
    data class FloatLit(val value: Double) : Token()
    data class StrLit(val parts: List<StringPartL>) : Token()

    data class Parens(val tokens: List<PToken>) : Token()
    data class Brackets(val tokens: List<PToken>) : Token()
    data class Braces(val tokens: List<PToken>) : Token()

    object At : Token()

    object Dot : Token() {
        override fun afterPostfix() = true
    }

    object Colon : Token() {
        override fun afterPostfix() = true
    }

    object DoubleColon : Token() {
        override fun afterPostfix() = true
    }

    object Semicolon : Token() {
        override fun afterPostfix() = true
        override fun beforePrefix() = true
    }

    object Comma : Token() {
        override fun afterPostfix() = true
        override fun beforePrefix() = true
    }

    object Or : Token()
    object And : Token()
    object Eq : Token()
    object NotEq : Token()
    object RefEq : Token()
    object NotRefEq : Token()
    object Less : Token()
    object Greater : Token()
    object LessOrEq : Token()
    object GreaterOrEq : Token()
    object Is : Token()
    object In : Token()
    object RangeTo : Token() {
        override fun afterPostfix() = true
        override fun beforePrefix() = true
    }

    object Plus : Token()
    object Minus : Token()
    object Times : Token()
    object Div : Token()
    object Rem : Token()
    object As : Token()

    object Assign : Token()
    object PlusAssign : Token()
    object MinusAssign : Token()
    object TimesAssign : Token()
    object DivAssign : Token()
    object RemAssign : Token()

    object QuestionMark : Token()
    object ExclamationMark : Token()

    object Arrow : Token()
    object Spread : Token()
    object Wildcard : Token()

    object Fun : Token()
    object Val : Token()
    object Var : Token()
    object Class : Token()
    object Object : Token()
    object Interface : Token()

    object Super : Token()
    object When : Token()
    object If : Token()
    object Else : Token()
    object Do : Token()
    object Catch : Token()
    object Defer : Token()
    object Break : Token()
    object Return : Token()
    object Throw : Token()

    object Import : Token()

    object Mod : Token()
    object Crate : Token()

    object Whitespace : Token() {
        override fun canIgnore() = true
    }

    object Comment : Token() {
        override fun canIgnore() = true
    }

    object EOL : Token() {
        override fun canIgnore() = true
    }

    object End : Token() {
        override fun afterPostfix() = true
    }

    data class Error(val error: String) : Token()
}

typealias PToken = Positioned<Token>

val trueToken = Token.BoolLit(true)
val falseToken = Token.BoolLit(false)