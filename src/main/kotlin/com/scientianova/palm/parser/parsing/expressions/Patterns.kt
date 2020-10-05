package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.invalidDoubleDeclaration
import com.scientianova.palm.errors.invalidPattern
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Expr
import com.scientianova.palm.parser.data.expressions.PPattern
import com.scientianova.palm.parser.data.expressions.Pattern
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map

enum class DecHandling {
    None, Val, Var
}

fun parsePattern(parser: Parser, handling: DecHandling): PPattern = when (parser.current) {
    Token.Wildcard -> parser.advance().end(Pattern.Wildcard)
    Token.LParen -> parseTuplePattern(parser, handling)
    Token.LBrace -> parseRecordPattern(parser, handling)
    Token.Dot -> parseEnumPattern(parser, handling)
    Token.Is -> parseTypePattern(parser)
    Token.Var -> if (handling == DecHandling.None) {
        parsePattern(parser.advance(), DecHandling.Var)
    } else {
        parser.err(invalidDoubleDeclaration)
    }
    Token.Val -> if (handling == DecHandling.None) {
        parsePattern(parser.advance(), DecHandling.Var)
    } else {
        parser.err(invalidDoubleDeclaration)
    }
    else -> {
        val expr = requireSubExpr(parser)
        val value = expr.value
        if (value is Expr.Ident) when (handling) {
            DecHandling.Var -> Pattern.Dec(value.name, true).at(expr.start, expr.next)
            DecHandling.Val -> Pattern.Dec(value.name, false).at(expr.start, expr.next)
            DecHandling.None -> expr.map(Pattern::Expr)
        } else {
            expr.map(Pattern::Expr)
        }
    }
}

fun parsePatternNoExpr(parser: Parser, handling: DecHandling): PPattern = when (parser.current) {
    Token.Wildcard -> parser.advance().end(Pattern.Wildcard)
    Token.LParen -> parseTuplePattern(parser, handling)
    Token.LBrace -> parseRecordPattern(parser, handling)
    Token.Dot -> parseEnumPattern(parser, handling)
    Token.Is -> parseTypePattern(parser)
    else -> parser.err(invalidPattern)
}

private fun parseTypePattern(parser: Parser): PPattern {
    val start = parser.Marker()
    parser.advance()

    val type = parseType(parser)

    return start.end(Pattern.Type(type))
}

private fun parseTuplePatternBody(parser: Parser, handling: DecHandling): List<PPattern> = recBuildList {
    if (parser.current == Token.RParen) {
        return this
    } else {
        add(parsePattern(parser, handling))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

private fun parseTuplePattern(parser: Parser, handling: DecHandling): PPattern {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val list = parseTuplePatternBody(parser, handling)

    parser.advance()
    marker.revertFlags()

    return if (list.size == 1) {
        list[0]
    } else {
        marker.end(Pattern.Tuple(list))
    }
}

private fun parseEnumPattern(parser: Parser, handling: DecHandling): PPattern {
    val start = parser.Marker()
    parser.advance()

    val name = parseIdent(parser)

    return when (parser.current) {
        Token.LParen -> {
            parser.advance()
            parser.trackNewline = false
            parser.excludeCurly = false

            val list = parseTuplePatternBody(parser, handling)

            parser.advance()
            start.revertFlags()

            start.end(Pattern.EnumTuple(name, list))
        }
        Token.LBrace -> {
            parser.advance()
            parser.excludeCurly = false
            parser.trackNewline = false

            val list = parseRecordPatternBody(parser, handling)

            start.revertFlags()
            parser.advance()

            start.end(Pattern.Enum(name, list))
        }
        else -> start.end(Pattern.EnumTuple(name, emptyList()))
    }
}

private fun convertName(name: PString, handling: DecHandling) = when (handling) {
    DecHandling.None -> Pattern.Wildcard.at(name.start, name.next)
    DecHandling.Var -> Pattern.Dec(name.value, true).at(name.start, name.next)
    DecHandling.Val -> Pattern.Dec(name.value, false).at(name.start, name.next)
}

private fun parseRecordPatternBody(parser: Parser, handling: DecHandling): List<Pair<PString, PPattern>> =
    recBuildList {
        if (parser.current == Token.RBrace) {
            return this
        } else {
            val name = parseIdent(parser)
            when (parser.current) {
                Token.Comma -> {
                    add(name to convertName(name, handling))
                    parser.advance()
                }
                Token.RBrace -> {
                    add(name to convertName(name, handling))
                    return this
                }
                Token.Colon -> {
                    val pattern = parsePattern(parser.advance(), handling)
                    add(name to pattern)

                    when (parser.current) {
                        Token.Comma -> parser.advance()
                        Token.RBrace -> return this
                        else -> parser.err(unclosedParenthesis)
                    }
                }
                else -> parser.err(unclosedParenthesis)
            }
        }
    }


private fun parseRecordPattern(parser: Parser, handling: DecHandling): PPattern {
    val marker = parser.Marker()

    parser.advance()
    parser.excludeCurly = false
    parser.trackNewline = false

    val list = parseRecordPatternBody(parser, handling)

    marker.revertFlags()
    parser.advance()

    return marker.end(Pattern.Record(list))
}