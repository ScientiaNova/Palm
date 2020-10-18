package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.data.top.AnnotationType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.parsing.expressions.parseBinOps
import com.scientianova.palm.parser.parsing.expressions.requireSubExpr
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class StringPart {
    data class String(val string: kotlin.String) : StringPart()
    data class Expr(val expr: PExpr) : StringPart()
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

    open val annotationType get() = AnnotationType.Normal
    open val decModifier: DecModifier? get() = null
    open val precedence get() = -1

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
        override val precedence get() = 1
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Or(left, right).at(left.start, right.next)
    }

    object And : Token() {
        override val precedence get() = 2
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.And(left, right).at(left.start, right.next)
    }

    object Eq : Token() {
        override val precedence get() = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Eq(left, right).at(left.start, right.next)
    }

    object NotEq : Token() {
        override val precedence get() = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.NotEq(left, right).at(left.start, right.next)
    }

    object RefEq : Token() {
        override val precedence get() = 3
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.RefEq(left, right).at(left.start, right.next)
    }

    object NotRefEq : Token() {
        override val precedence get() = 3
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.NotRefEq(left, right).at(left.start, right.next)
    }

    object Less : Token() {
        override val precedence get() = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.LT, right).at(left.start, right.next)
    }

    object Greater : Token() {
        override val precedence get() = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.GT, right).at(left.start, right.next)
    }

    object LessOrEq : Token() {
        override val precedence get() = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.LTEq, right).at(left.start, right.next)
    }

    object GreaterOrEq : Token() {
        override val precedence get() = 4
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.GTEq, right).at(left.start, right.next)
    }

    object Is : Token() {
        override val precedence get() = 5
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = requireType(parser)
            return Expr.TypeCheck(left, type).at(left.start, type.next)
        }
    }

    object Elvis : Token() {
        override val precedence get() = 6
        override fun convertBinary(left: PExpr, right: PExpr) = Expr.Elvis(left, right).at(left.start, right.next)
    }

    object RangeTo : Token() {
        override val precedence get() = 7
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.RangeTo, right).at(left.start, right.next)
    }

    object Plus : Token() {
        override val precedence get() = 8
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Plus, right).at(left.start, right.next)
    }

    object Minus : Token() {
        override val precedence get() = 8
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Minus, right).at(left.start, right.next)
    }

    object Times : Token() {
        override val precedence get() = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Times, right).at(left.start, right.next)
    }

    object Div : Token() {
        override val precedence get() = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Div, right).at(left.start, right.next)
    }

    object Rem : Token() {
        override val precedence get() = 9
        override fun convertBinary(left: PExpr, right: PExpr) =
            Expr.Binary(left, BinaryOp.Rem, right).at(left.start, right.next)
    }

    object As : Token() {
        override val precedence get() = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = requireType(parser)
            return Expr.SafeCast(left, type).at(left.start, type.next)
        }
    }

    object NullableAs : Token() {
        override val precedence get() = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = requireType(parser)
            return Expr.NullableCast(left, type).at(left.start, type.next)
        }
    }

    object UnsafeAs : Token() {
        override val precedence get() = 10
        override fun handleBinary(parser: Parser, left: PExpr, precedence: kotlin.Int): PExpr {
            val type = requireType(parser)
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

    object In : Token() {
        override fun identString() = "in"
        override val decModifier get() = DecModifier.In
    }

    object Out : Token() {
        override fun identString() = "out"
        override val decModifier get() = DecModifier.Out
    }

    object Arrow : Token()
    object Spread : Token()
    object Wildcard : Token()
    object Comma : Token()
    object Fun : Token()
    object Val : Token()
    object Var : Token()

    object Const : Token() {
        override fun identString() = "const"
    }

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

    object Mixin : Token() {
        override fun identString() = "mixin"
    }

    object Trait : Token() {
        override fun identString() = "trait"
    }

    object Impl : Token() {
        override fun identString() = "impl"
    }

    object Type : Token() {
        override fun identString() = "type"
    }

    object Extend : Token() {
        override fun identString() = "extends"
    }

    object Leaf : Token() {
        override fun identString() = "leaf"
        override val decModifier get() = DecModifier.Leaf
    }

    object Abstract : Token() {
        override fun identString() = "abstract"
        override val decModifier get() = DecModifier.Abstract
    }

    object Static : Token() {
        override fun identString() = "static"
        override val decModifier get() = DecModifier.Static
    }

    object Inline : Token() {
        override fun identString() = "inline"
        override val decModifier get() = DecModifier.Inline
    }

    object Tailrec : Token() {
        override fun identString() = "tailrec"
        override val decModifier get() = DecModifier.Tailrec
    }

    object Public : Token() {
        override fun identString() = "public"
        override val decModifier get() = DecModifier.Public
    }

    object Protected : Token() {
        override fun identString() = "protected"
        override val decModifier get() = DecModifier.Protected
    }

    object Private : Token() {
        override fun identString() = "private"
        override val decModifier get() = DecModifier.Private
    }

    object Internal : Token() {
        override fun identString() = "internal"
        override val decModifier get() = DecModifier.Internal
    }

    object Noinline : Token() {
        override fun identString() = "noinline"
        override val decModifier get() = DecModifier.NoInline
    }

    object Crossinline : Token() {
        override fun identString() = "crossinline"
        override val decModifier get() = DecModifier.CrossInline
    }

    object Lateinit : Token() {
        override fun identString() = "lateinit"
        override val decModifier get() = DecModifier.Lateinit
    }

    object Override : Token() {
        override fun identString() = "override"
        override val decModifier get() = DecModifier.Override
    }

    object Partial : Token() {
        override fun identString() = "partial"
        override val decModifier get() = DecModifier.Partial
    }

    object Annotation : Token() {
        override fun identString() = "annotation"
        override val decModifier get() = DecModifier.Ann
    }

    object Suspend : Token() {
        override fun identString() = "suspend"
    }

    object Get : Token() {
        override fun identString() = "get"
        override val annotationType get() = AnnotationType.Get
    }

    object Set : Token() {
        override fun identString() = "set"
        override val annotationType get() = AnnotationType.Set
    }

    object File : Token() {
        override fun identString() = "file"
        override val annotationType get() = AnnotationType.File
    }

    object Field : Token() {
        override fun identString() = "field"
        override val annotationType get() = AnnotationType.Field
    }

    object Delegate : Token() {
        override fun identString() = "delegate"
        override val annotationType get() = AnnotationType.Delegate
    }

    object Property : Token() {
        override fun identString() = "property"
        override val annotationType get() = AnnotationType.Property
    }

    object Param : Token() {
        override fun identString() = "param"
        override val annotationType get() = AnnotationType.Param
    }

    object Setparam : Token() {
        override fun identString() = "setparam"
        override val annotationType get() = AnnotationType.SetParam
    }

    object Blank : Token() {
        override fun identString() = "blank"
        override val decModifier get() = DecModifier.Blank
    }

    object Using : Token() {
        override fun identString() = "using"
        override val decModifier get() = DecModifier.Using
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
    object Defer : Token()
    object Loop : Token()
    object Break : Token()
    object Continue : Token()
    object Return : Token()
    object Throw : Token()
    object Guard : Token()
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

    object With : Token() {
        override fun identString() = "with"
    }

    object On : Token() {
        override fun identString() = "on"
    }

    object Init : Token() {
        override fun identString() = "init"
    }

    object Constructor : Token() {
        override fun identString() = "constructor"
        override val annotationType get() = AnnotationType.Constructor
    }

    object Operator : Token() {
        override fun identString() = "operator"
        override val decModifier get() = DecModifier.Operator
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

    data class MetadataComment(val content: String) : Token()

    data class Error(val error: PError) : Token()
}

fun Token.isIdentifier() = identString().isNotEmpty()

typealias PToken = Pair<Token, StringPos>

val trueToken = Token.Bool(true)
val falseToken = Token.Bool(false)
val emptyStr = Token.Str(listOf(StringPart.String("")))

fun PalmError.token(startPos: StringPos, nextPos: StringPos = startPos + 1) =
    PToken(Token.Error(this.at(startPos, nextPos)), nextPos)