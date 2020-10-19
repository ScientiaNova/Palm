package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.missingScope
import com.scientianova.palm.errors.unclosedScope
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.recBuildList

fun parseStatements(parser: Parser): ExprScope = recBuildList {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        else -> {
            add(requireBinOps(parser))
            val sep = parser.current
            when {
                sep == Token.Semicolon -> parser.advance()
                sep == Token.RBrace -> return this
                parser.lastNewline -> {

                }
                else -> parser.err(unclosedScope)
            }
        }
    }
}

fun parseScopeBody(parser: Parser): ExprScope {
    parser.advance()

    val scope = parser.withFlags(trackNewline = true, excludeCurly = false) {
        parseStatements(parser)
    }

    parser.advance()

    return scope
}

fun parseScope(parser: Parser) = if (parser.current == Token.LBrace) {
    parseScopeBody(parser)
} else {
    null
}

fun requireScope(parser: Parser) = if (parser.current == Token.LBrace) {
    parseScopeBody(parser)
} else {
    parser.err(missingScope)
}