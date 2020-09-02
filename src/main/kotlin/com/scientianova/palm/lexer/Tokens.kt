package com.scientianova.palm.lexer

import com.scientianova.palm.util.Positioned

sealed class StringPart {
    data class Regular(val string: String) : StringPart()
    data class SingleToken(val token: PToken) : StringPart()
    data class List(val list: TokenList) : StringPart()
}

sealed class Token {
    open fun identString() = ""

    data class Ident(val name: String) : Token() {
        override fun identString() = name
    }

    data class Bool(val value: Boolean) : Token()
    data class Char(val value: kotlin.Char) : Token()
    data class Byte(val value: kotlin.Byte) : Token()
    data class Short(val value: kotlin.Short) : Token()
    data class Int(val value: kotlin.Int) : Token()
    data class Long(val value: kotlin.Long) : Token()
    data class Float(val value: kotlin.Float) : Token()
    data class Double(val value: kotlin.Double) : Token()
    data class Str(val parts: List<StringPart>) : Token()

    data class Label(val name: String) : Token()

    object LParen : Token()
    object RParen : Token()
    object LBrace : Token()
    object RBrace : Token()
    object LBracket : Token()
    object RBracket : Token()
    object Dot : Token()
    object RangeTo : Token()
    object RangeFrom : Token()
    object RangeUntil : Token()
    object SafeAccess : Token()
    object Colon : Token()
    object DoubleColon : Token()
    object Semicolon : Token()
    object And : Token()
    object Or : Token()
    object Less : Token()
    object Greater : Token()
    object LessOrEq : Token()
    object GreaterOrEq : Token()
    object Elvis : Token()
    object Plus : Token()
    object Minus : Token()
    object Times : Token()
    object Div : Token()
    object Rem : Token()
    object Eq : Token()
    object NotEq : Token()
    object RefEq : Token()
    object NotRefEq : Token()
    object Assign : Token()
    object PlusAssign : Token()
    object MinusAssign : Token()
    object TimesAssign : Token()
    object DivAssign : Token()
    object RemAssign : Token()
    object QuestionMark : Token()
    object DoubleExclamation : Token()
    object UnaryPlus : Token()
    object UnaryMinus : Token()
    object Not : Token()
    object In : Token()
    object Out : Token()
    object Arrow : Token()
    object Spread : Token()
    object Wildcard : Token()
    object Comma : Token()
    object At : Token()
    object Fun : Token()
    object Val : Token()
    object Var : Token()
    object Class : Token() {
        override fun identString() = "class"
    }

    object Object : Token()
    object Enum : Token() {
        override fun identString() = "enum"
    }

    object Record : Token() {
        override fun identString() = "record"
    }

    object Type : Token() {
        override fun identString() = "type"
    }

    object Extend : Token() {
        override fun identString() = "extends"
    }

    object Leaf : Token() {
        override fun identString() = "leaf"
    }

    object Abstract : Token() {
        override fun identString() = "abstract"
    }

    object Inline : Token() {
        override fun identString() = "inline"
    }

    object Tailrec : Token() {
        override fun identString() = "tailrec"
    }

    object Public : Token() {
        override fun identString() = "public"
    }

    object Protected : Token() {
        override fun identString() = "protected"
    }

    object Private : Token() {
        override fun identString() = "private"
    }

    object Internal : Token() {
        override fun identString() = "internal"
    }

    object Noinline : Token() {
        override fun identString() = "noinline"
    }

    object Crossinline : Token() {
        override fun identString() = "crossinline"
    }

    object Lateinit : Token() {
        override fun identString() = "lateinit"
    }

    object Given : Token() {
        override fun identString() = "given"
    }

    object Override : Token() {
        override fun identString() = "override"
    }

    object Suspend : Token() {
        override fun identString() = "suspend"
    }

    object Get : Token() {
        override fun identString() = "get"
    }

    object Set : Token() {
        override fun identString() = "set"
    }

    object This : Token()
    object Super : Token()
    object Null : Token()
    object When : Token()
    object If : Token()
    object Else : Token()
    object For : Token()
    object While : Token()
    object Do : Token()
    object Loop : Token()
    object Break : Token()
    object Continue : Token()
    object Return : Token()
    object Throw : Token()
    object Guard : Token()
    object Using : Token()
    object Nobreak : Token()
    object Fallthrough : Token()
    object As : Token()
    object NullableAs : Token()
    object UnsafeAs : Token()
    object Is : Token()
    object Import : Token() {
        override fun identString() = "import"
    }

    object Package : Token() {
        override fun identString() = "package"
    }

    object By : Token() {
        override fun identString() = "by"
    }

    object With : Token() {
        override fun identString() = "with"
    }

    object Where : Token() {
        override fun identString() = "where"
    }

    object Init : Token() {
        override fun identString() = "init"
    }

    object Constructor : Token() {
        override fun identString() = "constructor"
    }

    object EOL : Token()
    object EOF : Token()
}

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)
val emptyStr = Token.Str(listOf(StringPart.Regular("")))

typealias PToken = Positioned<Token>
typealias TokenList = List<PToken>