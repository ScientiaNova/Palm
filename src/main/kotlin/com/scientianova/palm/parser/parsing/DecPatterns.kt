package com.scientianova.palm.parser.parsing

import com.scientianova.palm.errors.invalidPattern
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.isIdentifier
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.DecPattern
import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.recBuildList

fun parseDecPattern(parser: Parser): PDecPattern {
    val token = parser.current
    return when {
        token.isIdentifier() -> parser.advance().end(DecPattern.Name(token.identString()))
        token == Token.Wildcard -> parser.advance().end(DecPattern.Wildcard)
        token == Token.LParen -> parseDecTuple(parser)
        token == Token.LBrace -> TODO()
        else -> parser.err(invalidPattern)
    }
}

private fun parseDecTupleBody(parser: Parser): List<PDecPattern> = recBuildList<PDecPattern> {
    if (parser.current == Token.RParen) {
        return this
    } else {
        add(parseDecPattern(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

fun parseDecTuple(parser: Parser): PDecPattern {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val list = parseDecTupleBody(parser)

    parser.advance()
    marker.revertFlags()

    return if (list.size == 1) {
        list[0]
    } else {
        marker.end(DecPattern.Tuple(list))
    }
}