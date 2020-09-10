package com.scientianova.palm.parser.parsing

import com.scientianova.palm.errors.missingElseInGuard
import com.scientianova.palm.errors.missingScope
import com.scientianova.palm.errors.unclosedScope
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.expressions.GuardStatement
import com.scientianova.palm.parser.data.expressions.ScopeStatement
import com.scientianova.palm.parser.recBuildList

fun parseStatement(parser: Parser): ScopeStatement = when (parser.current) {
    Token.Val -> TODO()
    Token.Var -> TODO()
    Token.Using -> TODO()
    Token.Guard -> parseGuard(parser)
    else -> TODO()
}

fun parseGuard(parser: Parser): ScopeStatement {
    parser.advance()

    val conditions = parseConditions(parser)
    if (parser.current != Token.Else) parser.err(missingElseInGuard)
    val scope = requireScope(parser.advance())

    return GuardStatement(conditions, scope)
}

fun parseStatements(parser: Parser): ExprScope = recBuildList<ScopeStatement> {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        else -> {
            add(parseStatement(parser))
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
    val marker = parser.Marker()
    parser.advance()

    parser.trackNewline = true
    parser.excludeCurly = false

    val scope = parseStatements(parser)

    parser.advance()
    marker.revertFlags()

    return scope
}

fun requireScope(parser: Parser) = if (parser.current == Token.LBrace) {
    parseScopeBody(parser)
} else {
    parser.err(missingScope)
}