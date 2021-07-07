package com.palmlang.palm.lexer

import com.palmlang.palm.ast.expressions.Scope
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.StringPartP
import com.palmlang.palm.parser.top.parseStatements
import com.palmlang.palm.util.Positioned
import com.palmlang.palm.util.StringPos
import com.palmlang.palm.util.at

sealed class StringPartL {
    abstract fun parse(parser: Parser): StringPartP
    data class String(val string: kotlin.String) : StringPartL() {
        override fun parse(parser: Parser) = StringPartP.String(string)
    }

    data class Expr(val startPos: StringPos, val tokens: List<PToken>) : StringPartL() {
        override fun parse(parser: Parser): StringPartP = StringPartP.Expr(
            parser.scopedOf(tokens).parseStatements().let { Scope(null, null, it) }.at(startPos, tokens.last().next)
        )
    }
}

sealed interface Token {
    fun canIgnore() = false
    fun afterPostfix() = canIgnore()
    fun beforePrefix() = canIgnore()

    data class Ident(val name: String, val backticked: Boolean) : Token

    object NullLit : Token
    data class BoolLit(val value: Boolean) : Token
    data class CharLit(val value: Char) : Token
    data class IntLit(val value: Long) : Token
    data class FloatLit(val value: Double) : Token
    data class StrLit(val parts: List<StringPartL>) : Token

    data class Parens(val tokens: List<PToken>) : Token
    data class Brackets(val tokens: List<PToken>) : Token
    data class Braces(val tokens: List<PToken>) : Token

    object At : Token

    object Dot : Token {
        override fun afterPostfix() = true
    }

    object Colon : Token {
        override fun afterPostfix() = true
    }

    object DoubleColon : Token {
        override fun afterPostfix() = true
    }

    object Semicolon : Token {
        override fun afterPostfix() = true
        override fun beforePrefix() = true
    }

    object Comma : Token {
        override fun afterPostfix() = true
        override fun beforePrefix() = true
    }

    object Pipe : Token
    object And : Token
    object LogicalOr : Token
    object LogicalAnd : Token
    object Eq : Token
    object NotEq : Token
    object RefEq : Token
    object NotRefEq : Token
    object Less : Token
    object Greater : Token
    object LessOrEq : Token
    object GreaterOrEq : Token
    object Is : Token

    object Elvis : Token

    object Plus : Token
    object Minus : Token
    object Asterisk : Token
    object Div : Token
    object Rem : Token
    object As : Token

    object Assign : Token
    object PlusAssign : Token
    object MinusAssign : Token
    object TimesAssign : Token
    object DivAssign : Token
    object RemAssign : Token

    object QuestionMark : Token
    object ExclamationMark : Token

    object Arrow : Token
    object Wildcard : Token
    object Backslash : Token

    object Def : Token
    object Let : Token
    object Mut : Token
    object Class : Token
    object Object : Token
    object Interface : Token
    object Type : Token
    object Constructor : Token
    object Impl : Token

    object When : Token
    object Return : Token

    object Import : Token

    object Mod : Token
    object SuMod : Token

    object Whitespace : Token {
        override fun canIgnore() = true
    }

    object Comment : Token {
        override fun canIgnore() = true
    }

    object EOL : Token {
        override fun canIgnore() = true
    }

    object End : Token {
        override fun afterPostfix() = true
    }

    data class Error(val error: String) : Token
}

typealias PToken = Positioned<Token>

val trueToken = Token.BoolLit(true)
val falseToken = Token.BoolLit(false)