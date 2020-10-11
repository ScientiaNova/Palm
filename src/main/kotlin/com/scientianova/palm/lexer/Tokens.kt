package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parsing.expressions.parseBinOps
import com.scientianova.palm.parser.parsing.expressions.parseType
import com.scientianova.palm.parser.parsing.expressions.requireSubExpr
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class StringPart {
    data class String(val string: kotlin.String) : StringPart()
    data class Expr(val token: PExpr) : StringPart()
}

sealed class Token {
    open fun identString() = ""
    open fun canIgnore() = false
    open fun isPostfix() = false
    open fun isPrefix() = false
    open fun unaryOp(): UnaryOp = error("$this is not a unary operator.")
    open fun assignment(): AssignmentType? = null
    open fun convertBinary(left: PExpr, right: PExpr): PExpr = error("!??")
    open fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int) =
        convertBinary(left, parseBinOps(parser, requireSubExpr(parser), precedence + 1))

    open val precedence = -1

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

    object SafeAccess : Token()
    object Colon : Token()
    object DoubleColon : Token()
    object Semicolon : Token()

    object Or : Token() {
        override val precedence = 1
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Or(left, right).at(left.start, right.next)
    }

    object And : Token() {
        override val precedence = 2
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.And(left, right).at(left.start, right.next)
    }

    object Eq : Token() {
        override val precedence = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Eq(left, right).at(left.start, right.next)
    }

    object NotEq : Token() {
        override val precedence = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.NotEq(left, right).at(left.start, right.next)
    }

    object RefEq : Token() {
        override val precedence = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.RefEq(left, right).at(left.start, right.next)
    }

    object NotRefEq : Token() {
        override val precedence = 3
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.NotRefEq(left, right).at(left.start, right.next)
    }

    object Less : Token() {
        override val precedence = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.LT, right).at(left.start, right.next)
    }

    object Greater : Token() {
        override val precedence = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.GT, right).at(left.start, right.next)
    }

    object LessOrEq : Token() {
        override val precedence = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.LTEq, right).at(left.start, right.next)
    }

    object GreaterOrEq : Token() {
        override val precedence = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.GTEq, right).at(left.start, right.next)
    }

    object Is : Token() {
        override val precedence = 5
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = parseType(parser)
            return Expr.TypeCheck(left, type).at(left.start, type.next)
        }
    }

    object Elvis : Token() {
        override val precedence = 6
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Elvis(left, right).at(left.start, right.next)
    }

    object RangeTo : Token() {
        override val precedence = 7
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.RangeTo, right).at(left.start, right.next)
    }

    object Plus : Token() {
        override val precedence = 8
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Plus, right).at(left.start, right.next)
    }

    object Minus : Token() {
        override val precedence = 8
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Minus, right).at(left.start, right.next)
    }

    object Times : Token() {
        override val precedence = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Times, right).at(left.start, right.next)
    }

    object Div : Token() {
        override val precedence = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Div, right).at(left.start, right.next)
    }

    object Rem : Token() {
        override val precedence = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Rem, right).at(left.start, right.next)
    }

    object As : Token() {
        override val precedence = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = parseType(parser)
            return Expr.SafeCast(left, type).at(left.start, type.next)
        }
    }

    object NullableAs : Token() {
        override val precedence = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = parseType(parser)
            return Expr.NullableCast(left, type).at(left.start, type.next)
        }
    }

    object UnsafeAs : Token() {
        override val precedence = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = parseType(parser)
            return Expr.UnsafeCast(left, type).at(left.start, type.next)
        }
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

    object File : Token() {
        override fun identString() = "file"
    }

    object Field : Token() {
        override fun identString() = "field"
    }

    object Delegate : Token() {
        override fun identString() = "delegate"
    }

    object Property : Token() {
        override fun identString() = "property"
    }

    object Param : Token() {
        override fun identString() = "param"
    }

    object Setparam : Token() {
        override fun identString() = "setparam"
    }

    object Blank : Token() {
        override fun identString() = "blank"
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
    object Catch : Token()
    object With : Token()
    object Defer : Token()
    object Loop : Token()
    object Break : Token()
    object Continue : Token()
    object Return : Token()
    object Throw : Token()
    object Guard : Token()
    object Using : Token()
    object Nobreak : Token()
    object Fallthrough : Token()

    object Import : Token() {
        override fun identString() = "import"
    }

    object Package : Token() {
        override fun identString() = "package"
    }

    object By : Token() {
        override fun identString() = "by"
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

    data class Error(val error: PError) : Token()
}

fun Token.isIdentifier() = identString().isNotEmpty()

typealias PToken = Pair<Token, StringPos>

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)
val emptyStr = Token.Str(listOf(StringPart.String("")))

fun PalmError.token(startPos: StringPos, nextPos: StringPos = startPos + 1) =
    PToken(Token.Error(this.at(startPos, nextPos)), nextPos)