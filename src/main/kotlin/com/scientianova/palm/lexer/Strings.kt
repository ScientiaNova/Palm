package com.scientianova.palm.lexer

import com.scientianova.palm.errors.invalidInterpolation
import com.scientianova.palm.errors.missingDoubleQuote
import com.scientianova.palm.errors.unclosedMultilineString
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Expr
import com.scientianova.palm.parser.parsing.expressions.parseStatements
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.mapFirst

internal tailrec fun lexSingleLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null, '\n' -> missingDoubleQuote.token(pos)
    '"' -> Token.Str(parts + StringPart.String(builder.toString())) to pos + 1
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> {
                val parser = Parser(TokenStream(code, interPos))
                val scope = parseStatements(parser)
                val next = parser.nextPos
                lexSingleLineString(
                    code, next,
                    parts + StringPart.Expr(Expr.Scope(scope).at(interPos, next)),
                    builder.clear()
                )
            }
            '`' -> {
                val (ident, afterIdent) = lexTickedIdent(code, interPos + 1, StringBuilder())
                when (ident) {
                    is Token.Error -> ident to afterIdent
                    is Token.Ident -> lexSingleLineString(
                        code, afterIdent,
                        parts + StringPart.Expr(Expr.Ident(ident.identString()).at(interPos, afterIdent)),
                        builder.clear()
                    )
                    else -> error("!??")
                }
            }
            in identStartChars ->
                lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar)).mapFirst { TODO() }
            else ->
                lexSingleLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    '\\' -> when (val res = handleEscaped(code, pos + 1)) {
        is LexResult.Success ->
            lexSingleLineString(code, res.next, parts, builder.append(res.value))
        is LexResult.Error -> res.error.token(res.start, res.next)
    }
    else -> lexSingleLineString(code, pos + 1, parts, builder.append(char))
}

internal tailrec fun lexMultiLineString(
    code: String,
    pos: StringPos,
    parts: List<StringPart>,
    builder: StringBuilder
): PToken = when (val char = code.getOrNull(pos)) {
    null -> unclosedMultilineString.token(pos)
    '"' -> if (code.startsWith("\"\"", pos + 1)) {
        Token.Str(parts + StringPart.String(builder.toString())) to pos + 1
    } else lexMultiLineString(code, pos + 1, parts, builder.append('"'))
    '$' -> {
        val interPos = pos + 1
        when (val interChar = code.getOrNull(interPos)) {
            '{' -> {
                val parser = Parser(TokenStream(code, interPos))
                val scope = parseStatements(parser)
                val next = parser.nextPos
                lexMultiLineString(
                    code, next,
                    parts + StringPart.Expr(Expr.Scope(scope).at(interPos, next)),
                    builder.clear()
                )
            }
            '`' -> {
                val (ident, afterIdent) = lexTickedIdent(code, interPos + 1, StringBuilder())
                when (ident) {
                    is Token.Error -> ident to afterIdent
                    is Token.Ident -> lexMultiLineString(
                        code, afterIdent,
                        parts + StringPart.Expr(Expr.Ident(ident.identString()).at(interPos, afterIdent)),
                        builder.clear()
                    )
                    else -> error("!??")
                }
            }
            in identStartChars -> {
                val (token, afterToken) = lexNormalIdent(code, interPos + 1, StringBuilder().append(interChar))
                when (token) {
                    in identTokens -> lexMultiLineString(
                        code, afterToken,
                        parts + StringPart.Expr(Expr.Ident(token.identString()).at(interPos, afterToken)),
                        builder.clear()
                    )
                    Token.This -> lexMultiLineString(
                        code, afterToken,
                        parts + StringPart.Expr(Expr.This(null).at(interPos, afterToken)),
                        builder.clear()
                    )
                    else -> invalidInterpolation.token(interPos, afterToken)
                }
            }
            else ->
                lexMultiLineString(code, pos + 1, parts, builder.append('$'))
        }
    }
    else -> lexMultiLineString(code, pos + 1, parts, builder.append(char))
}