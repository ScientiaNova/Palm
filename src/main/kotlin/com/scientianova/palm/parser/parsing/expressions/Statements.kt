package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.missingElseInGuard
import com.scientianova.palm.errors.missingScope
import com.scientianova.palm.errors.unclosedScope
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.recBuildList

private fun parseStatement(parser: Parser): ScopeStatement = when (parser.current) {
    Token.Val -> parseDecStatement(parser, false)
    Token.Var -> parseDecStatement(parser, true)
    Token.Using -> UsingStatement(requireBinOps(parser.advance()))
    Token.Guard -> parseGuard(parser)
    Token.Defer -> DeferStatement(requireScope(parser.advance()))
    else -> {
        val subExpr = requireSubExpr(parser)
        val assignment = parser.current.assignment()
        if (assignment == null) {
            ExprStatement(parseBinOps(parser, subExpr))
        } else {
            AssignStatement(subExpr, assignment, requireBinOps(parser.advance()))
        }
    }
}

private fun parseDecStatement(parser: Parser, mutable: Boolean): ScopeStatement {
    val pattern = requireDecPattern(parser.advance())
    val type = parseTypeAnn(parser)
    val expr = parseEqExpr(parser)
    return DecStatement(pattern, mutable, type, expr)
}

private fun parseGuard(parser: Parser): ScopeStatement {
    parser.advance()

    val conditions = parseConditions(parser)
    if (parser.current != Token.Else) parser.err(missingElseInGuard)
    val scope = requireScope(parser.advance())

    return GuardStatement(conditions, scope)
}

fun parseStatements(parser: Parser): ExprScope = recBuildList {
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