package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

sealed class StringPart {
    data class String(val string: kotlin.String) : StringPart()
    data class Expr(val expr: List<PToken>) : StringPart()
}

sealed class Token {
    open fun canIgnore() = false

    data class Ident(val name: String, val backticked: Boolean) : Token()

    object NullLit : Token()
    data class BoolLit(val value: Boolean) : Token()
    data class CharLit(val value: Char) : Token()
    data class IntLit(val value: Long) : Token()
    data class FloatLit(val value: Double) : Token()
    data class StrLit(val parts: List<StringPart>) : Token()

    data class Parens(val token: List<PToken>) : Token()
    data class Brackets(val token: List<PToken>) : Token()
    data class Braces(val token: List<PToken>) : Token()

    object At : Token()
    object Dot : Token()
    object Colon : Token()
    object DoubleColon : Token()
    object Semicolon : Token()

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
    object RangeTo : Token()
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
    object Comma : Token()

    object Fun : Token()
    object Val : Token()
    object Var : Token()
    object Class : Token()
    object Object : Token()
    object Enum : Token()

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

    object Whitespace : Token() {
        override fun canIgnore() = true
    }

    object Comment : Token() {
        override fun canIgnore() = true
    }

    object EOL : Token() {
        override fun canIgnore() = true
    }

    data class MetadataComment(val content: String) : Token()

    data class Error(val error: String) : Token()
}

data class PToken(val token: Token, val next: StringPos)
fun Token.till(next: StringPos) = PToken(this, next)

val trueToken = Token.BoolLit(true)
val falseToken = Token.BoolLit(false)