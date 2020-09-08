package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.missingLabelName
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.isIdentifier
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.alsoAdvance
import com.scientianova.palm.parser.data.expressions.Expr
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.parseErr
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at

fun parseTerm(parser: Parser): PExpr? {
    val (token, start, next) = parser.current
    return when {
        token.isIdentifier() -> Expr.Ident(token.identString()).at(start, next).alsoAdvance(parser)
        token is Token.Byte -> Expr.Byte(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Short -> Expr.Short(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Int -> Expr.Int(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Long -> Expr.Long(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Float -> Expr.Float(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Double -> Expr.Double(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Bool -> Expr.Bool(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Char -> Expr.Char(token.value).at(start, next).alsoAdvance(parser)
        token is Token.Str -> TODO()
        token == Token.Null -> Expr.Null.at(start, next).alsoAdvance(parser)
        token == Token.This -> Expr.This(parseLabelRef(parser.advance())).at(start, next)
        token == Token.Super -> Expr.Super(parseLabelRef(parser.advance())).at(start, next)
        token == Token.Return -> TODO()
        token == Token.Break -> TODO()
        token == Token.Continue -> Expr.Continue(parseLabelRef(parser.advance())).at(start, next)
        token == Token.If -> TODO()
        token == Token.When -> TODO()
        token == Token.For -> TODO()
        token == Token.While -> TODO()
        token == Token.Loop -> TODO()
        token == Token.LBrace -> TODO()
        token == Token.LBracket -> TODO()
        token == Token.LParen -> TODO()
        token == Token.Object -> TODO()
        token.isPrefix() -> Expr.Unary(token.unaryOp(), TODO()).at(start, next)
        else -> null
    }
}

fun parseLabelRef(parser: Parser): PString? = if (parser.current.value == Token.At) {
    val (ident, start, next) = parser.rawLookup(2)
    if (ident.isIdentifier()) {
        parser.advance().advance()
        ident.identString().at(start, next)
    } else {
        parseErr(missingLabelName, parser.rawLookup(2))
    }
} else {
    null
}

