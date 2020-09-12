package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parsing.expressions.parseType
import com.scientianova.palm.parser.parsing.expressions.requireSubExpr
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import java.util.*

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
    open fun handleBinary(second: PExpr, first: PExpr): PExpr = error("!??")
    open fun handleBinaryAppend(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>) {
        opStack.push(this)
        operandStack.push(requireSubExpr(parser))
    }
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

    object Or : Token() {
        override val precedence = 1
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Or(first, second).at(first.start, second.next)
    }

    object And : Token() {
        override val precedence = 2
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.And(first, second).at(first.start, second.next)
    }

    object Eq : Token() {
        override val precedence = 3
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Eq(first, second).at(first.start, second.next)
    }

    object NotEq : Token() {
        override val precedence = 3
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.NotEq(first, second).at(first.start, second.next)
    }

    object RefEq : Token() {
        override val precedence = 3
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.RefEq(first, second).at(first.start, second.next)
    }

    object NotRefEq : Token() {
        override val precedence = 3
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.NotRefEq(first, second).at(first.start, second.next)
    }

    object Less : Token() {
        override val precedence = 4
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.LT, second).at(first.start, second.next)
    }

    object Greater : Token() {
        override val precedence = 4
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.GT, second).at(first.start, second.next)
    }

    object LessOrEq : Token() {
        override val precedence = 4
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.LTEq, second).at(first.start, second.next)
    }

    object GreaterOrEq : Token() {
        override val precedence = 4
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.GTEq, second).at(first.start, second.next)
    }

    object Is : Token() {
        override val precedence = 5
        override fun handleBinaryAppend(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>) {
            val left = operandStack.pop()
            val type = parseType(parser)
            operandStack.push(Expr.TypeCheck(left, type).at(left.start, type.next))
        }
    }

    object Elvis : Token() {
        override val precedence = 6
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Elvis(first, second).at(first.start, second.next)
    }

    object RangeTo : Token() {
        override val precedence = 7
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.RangeTo, second).at(first.start, second.next)
    }

    object Plus : Token() {
        override val precedence = 8
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.Plus, second).at(first.start, second.next)
    }

    object Minus : Token() {
        override val precedence = 8
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.Minus, second).at(first.start, second.next)
    }

    object Times : Token() {
        override val precedence = 9
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.Times, second).at(first.start, second.next)
    }

    object Div : Token() {
        override val precedence = 9
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.Div, second).at(first.start, second.next)
    }

    object Rem : Token() {
        override val precedence = 9
        override fun handleBinary(second: PExpr, first: PExpr) = Expr.Binary(first, BinaryOp.Rem, second).at(first.start, second.next)
    }

    object As : Token() {
        override val precedence = 10
        override fun handleBinaryAppend(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>) {
            val left = operandStack.pop()
            val type = parseType(parser)
            operandStack.push(Expr.SafeCast(left, type).at(left.start, type.next))
        }
    }

    object NullableAs : Token() {
        override val precedence = 10
        override fun handleBinaryAppend(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>) {
            val left = operandStack.pop()
            val type = parseType(parser)
            operandStack.push(Expr.NullableCast(left, type).at(left.start, type.next))
        }
    }

    object UnsafeAs : Token() {
        override val precedence = 10
        override fun handleBinaryAppend(parser: Parser, opStack: Stack<Token>, operandStack: Stack<PExpr>) {
            val left = operandStack.pop()
            val type = parseType(parser)
            operandStack.push(Expr.UnsafeCast(left, type).at(left.start, type.next))
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

    object PlaceHolder : Token() {
        override val precedence = -100
    }

    data class Error(val error: PError) : Token()
}

fun Token.isIdentifier() = identString().isNotEmpty()

typealias PToken = Pair<Token, StringPos>

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)
val emptyStr = Token.Str(listOf(StringPart.String("")))

fun PalmError.token(startPos: StringPos, nextPos: StringPos = startPos + 1) =
    PToken(Token.Error(this.at(startPos, nextPos)), nextPos)