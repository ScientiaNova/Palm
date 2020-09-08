package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.parser.data.expressions.AssignmentType
import com.scientianova.palm.parser.data.expressions.UnaryOp
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos

sealed class StringPart {
    data class Regular(val string: String) : StringPart()
    data class SingleToken(val token: Any /*TODO*/) : StringPart()
}

sealed class Token {
    open fun identString() = ""
    open fun canIgnore() = false
    open fun isPostfix() = false
    open fun isPrefix() = false
    open fun unaryOp(): UnaryOp = error("$this is not a unary operator.")
    open fun isBinary() = false
    open fun assignment(): AssignmentType? = null

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

    object At : Token()
    object LParen : Token()
    object RParen : Token()
    object LBrace : Token()
    object RBrace : Token()
    object LBracket : Token()
    object RBracket : Token()
    object Dot : Token()

    object RangeTo : Token() {
        override fun isBinary() = true
    }

    object RangeFrom : Token() {
        override fun unaryOp() = UnaryOp.RangeFrom
        override fun isPostfix() = true
    }

    object RangeUntil : Token() {
        override fun unaryOp() = UnaryOp.RangeUntil
        override fun isPrefix() = true
    }

    object SafeAccess : Token()
    object Colon : Token()
    object DoubleColon : Token()
    object Semicolon : Token()

    object And : Token() {
        override fun isBinary() = true
    }

    object Or : Token() {
        override fun isBinary() = true
    }

    object Less : Token() {
        override fun isBinary() = true
    }

    object Greater : Token() {
        override fun isBinary() = true
    }

    object LessOrEq : Token() {
        override fun isBinary() = true
    }

    object GreaterOrEq : Token() {
        override fun isBinary() = true
    }

    object Elvis : Token() {
        override fun isBinary() = true
    }

    object Plus : Token() {
        override fun isBinary() = true
    }

    object Minus : Token() {
        override fun isBinary() = true
    }

    object Times : Token() {
        override fun isBinary() = true
    }

    object Div : Token() {
        override fun isBinary() = true
    }

    object Rem : Token() {
        override fun isBinary() = true
    }

    object Eq : Token() {
        override fun isBinary() = true
    }

    object NotEq : Token() {
        override fun isBinary() = true
    }

    object RefEq : Token() {
        override fun isBinary() = true
    }

    object NotRefEq : Token() {
        override fun isBinary() = true
    }

    object Assign : Token() {
        override fun assignment() = AssignmentType.Normal
    }

    object PlusAssign : Token() {
        override fun assignment() = AssignmentType.Plus
    }

    object MinusAssign : Token() {
        override fun assignment() = AssignmentType.Minus
    }

    object TimesAssign : Token() {
        override fun assignment() = AssignmentType.Times
    }

    object DivAssign : Token() {
        override fun assignment() = AssignmentType.Div
    }

    object RemAssign : Token() {
        override fun assignment() = AssignmentType.Rem
    }

    object QuestionMark : Token()

    object NonNull : Token() {
        override fun unaryOp() = UnaryOp.NonNull
        override fun isPostfix() = true
    }

    object UnaryPlus : Token() {
        override fun unaryOp() = UnaryOp.Plus
        override fun isPrefix() = true
    }

    object UnaryMinus : Token() {
        override fun unaryOp() = UnaryOp.Minus
        override fun isPrefix() = true
    }

    object Not : Token() {
        override fun unaryOp() = UnaryOp.Not
        override fun isPrefix() = true
    }

    object In : Token()
    object Out : Token()
    object Arrow : Token()
    object Spread : Token()
    object Wildcard : Token()
    object Comma : Token()
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

    object As : Token() {
        override fun isBinary() = true
    }

    object NullableAs : Token() {
        override fun isBinary() = true
    }

    object UnsafeAs : Token() {
        override fun isBinary() = true
    }

    object Is : Token() {
        override fun isBinary() = true
    }

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

    object Operator : Token() {
        override fun identString() = "operator"
    }

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

    data class Error(val error: PalmError) : Token()
}

fun Token.isIdentifier() = identString().isNotEmpty()

typealias PToken = Positioned<Token>

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)
val emptyStr = Token.Str(listOf(StringPart.Regular("")))

fun PalmError.token(startPos: StringPos, nextPos: StringPos = startPos + 1) =
    PToken(Token.Error(this), startPos, nextPos)