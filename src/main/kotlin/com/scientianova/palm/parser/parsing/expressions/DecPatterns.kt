package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.invalidPattern
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.identTokens
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.DecPattern
import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.map

fun parseDecPattern(parser: Parser): PDecPattern? = when (val token = parser.current) {
    in identTokens -> parser.advance().end(DecPattern.Name(token.identString()))
    Token.Wildcard -> parser.advance().end(DecPattern.Wildcard)
    Token.LParen -> parseDecTuple(parser)
    Token.LBrace -> parseDecRecord(parser)
    else -> null
}

fun requireDecPattern(parser: Parser) = parseDecPattern(parser) ?: parser.err(invalidPattern)

private fun parseDecTupleBody(parser: Parser): List<PDecPattern>? = recBuildList {
    if (parser.current == Token.RParen) {
        return this
    } else {
        add(requireDecPattern(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> return null
        }
    }
}

private fun parseDecTuple(parser: Parser): PDecPattern? {
    val marker = parser.mark()
    parser.advance()

    val list = parseDecTupleBody(parser) ?: return null

    parser.advance()

    return if (list.size == 1) {
        list[0]
    } else {
        marker.end(DecPattern.Tuple(list))
    }
}

private fun parseDecRecordBody(parser: Parser): List<Pair<PString, PDecPattern>> = recBuildList {
    if (parser.current == Token.RBrace) {
        return this
    } else {
        val name = parseIdent(parser)
        when (parser.current) {
            Token.Comma -> {
                add(name to name.map(DecPattern::Name))
                parser.advance()
            }
            Token.RBrace -> {
                add(name to name.map(DecPattern::Name))
                return this
            }
            Token.Colon -> {
                val pattern = requireDecPattern(parser.advance())
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

private fun parseDecRecord(parser: Parser): PDecPattern {
    val marker = parser.mark()
    parser.advance()

    val list = parseDecRecordBody(parser)

    parser.advance()

    return marker.end(DecPattern.Record(list))
}