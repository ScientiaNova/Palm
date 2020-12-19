package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

sealed class StringPart {
    data class String(val string: kotlin.String) : StringPart()
    data class Expr(val expr: PToken) : StringPart()
}

sealed class Token {
    open fun canIgnore() = false

    data class Ident(val name: String, val backticked: Boolean) : Token()

    object Null : Token()
    data class Bool(val value: Boolean) : Token()
    data class Char(val value: kotlin.Char) : Token()
    data class Byte(val value: kotlin.Byte) : Token()
    data class Short(val value: kotlin.Short) : Token()
    data class Int(val value: kotlin.Int) : Token()
    data class Long(val value: kotlin.Long) : Token()
    data class Float(val value: kotlin.Float) : Token()
    data class Double(val value: kotlin.Double) : Token()
    data class Str(val parts: List<StringPart>) : Token()

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

    object EOF : Token()

    data class MetadataComment(val content: String) : Token()

    data class Error(val error: String) : Token()
}

typealias PToken = Pair<Token, StringPos>

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)