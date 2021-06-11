package com.palmlang.palm.parser.expressions

import com.palmlang.palm.ast.expressions.DecPattern
import com.palmlang.palm.ast.expressions.PDecPattern
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.at
import com.palmlang.palm.util.recBuildList

fun Parser.parseDecPattern(): com.palmlang.palm.ast.expressions.PDecPattern? = when (val token = current) {
    is Token.Mut -> {
        val start = pos
        val nested = advance().requireDecPattern()
        com.palmlang.palm.ast.expressions.DecPattern.Mut(nested).at(start, nested.next)
    }
    is Token.Ident -> com.palmlang.palm.ast.expressions.DecPattern.Name(token.name).end()
    is Token.Parens -> parseComponents(token.tokens)
    is Token.Brackets -> parseDecObject(token.tokens)
    Token.Wildcard -> com.palmlang.palm.ast.expressions.DecPattern.Wildcard.end()
    else -> null
}

fun Parser.requireDecPattern() = parseDecPattern() ?: run {
    err("Invalid pattern")
    com.palmlang.palm.ast.expressions.DecPattern.Wildcard.noPos().also { advance() }
}

private fun Parser.parseComponents(tokens: List<PToken>): com.palmlang.palm.ast.expressions.PDecPattern =
    parenthesizedOf(tokens).parseCommaSeparated(Parser::requireDecPattern) { components, trailing ->
        if (components.size == 1 && !trailing) com.palmlang.palm.ast.expressions.DecPattern.Parenthesized(components[0])
        else com.palmlang.palm.ast.expressions.DecPattern.Components(components)
    }.end()

private fun Parser.parseDecObjectBody(): List<Pair<PString, com.palmlang.palm.ast.expressions.PDecPattern?>> = recBuildList {
    if (current == Token.End) {
        return this
    } else {
        val ident = parseIdent()
        add(
            ident to if (current == Token.Colon) advance().requireDecPattern() else null
        )
        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }
}

private fun Parser.parseDecObject(tokens: List<PToken>): com.palmlang.palm.ast.expressions.PDecPattern =
    com.palmlang.palm.ast.expressions.DecPattern.Object(parenthesizedOf(tokens).parseDecObjectBody()).end()