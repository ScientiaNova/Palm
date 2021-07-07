package com.palmlang.palm.parser.expressions

import com.palmlang.palm.ast.expressions.ASTCommon
import com.palmlang.palm.ast.expressions.DecPattern
import com.palmlang.palm.ast.expressions.PDecPattern
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.at
import com.palmlang.palm.util.recBuildList

fun Parser.parseDecPattern(): PDecPattern? = when (val token = current) {
    is Token.Mut -> {
        val start = pos
        val nested = advance().requireDecPattern()
        DecPattern.Mut(nested).at(start, nested.next)
    }
    is Token.Ident -> DecPattern.Name(token.name).end()
    is Token.Parens -> parseComponents(token.tokens)
    is Token.Brackets -> parseDecObject(token.tokens)
    Token.Wildcard -> ASTCommon.Wildcard.end()
    else -> null
}

fun Parser.requireDecPattern() = parseDecPattern() ?: run {
    err("Invalid pattern")
    ASTCommon.Wildcard.noPos().also { advance() }
}

private fun Parser.parseComponents(tokens: List<PToken>): PDecPattern =
    parenthesizedOf(tokens).parseCommaSeparated(Parser::requireDecPattern) { components, trailing ->
        if (components.size == 1 && !trailing) DecPattern.Parenthesized(components[0])
        else DecPattern.Components(components)
    }.end()

private fun Parser.parseDecObjectBody(): List<Pair<PString, PDecPattern?>> = recBuildList {
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

private fun Parser.parseDecObject(tokens: List<PToken>): PDecPattern =
    DecPattern.Object(parenthesizedOf(tokens).parseDecObjectBody()).end()