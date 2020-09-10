package com.scientianova.palm.parser.parsing

import com.scientianova.palm.errors.invalidPattern
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PPattern
import com.scientianova.palm.parser.data.expressions.Pattern
import com.scientianova.palm.parser.recBuildList

enum class DecHandling {
    None, Val, Var
}

fun parsePattern(parser: Parser, handling: DecHandling): PPattern = when (parser.current) {
    Token.Wildcard -> parser.advance().end(Pattern.Wildcard)
    Token.LParen -> parseTuplePattern(parser, handling)
    Token.LBrace -> TODO()
    Token.Dot -> TODO()
    Token.Var -> TODO()
    Token.Val -> TODO()
    else -> TODO()
}

fun parsePatternNoExpr(parser: Parser, handling: DecHandling): PPattern = when (parser.current) {
    Token.Wildcard -> parser.advance().end(Pattern.Wildcard)
    Token.LParen -> parseTuplePattern(parser, handling)
    Token.LBrace -> TODO()
    Token.Dot -> TODO()
    else -> parser.err(invalidPattern)
}

private fun parseTuplePatternBody(parser: Parser, handling: DecHandling): List<PPattern> = recBuildList<PPattern> {
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

fun parseTuplePattern(parser: Parser, handling: DecHandling): PPattern {
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