package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.DecPattern
import com.scientianova.palm.parser.data.expressions.PDecPattern
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

fun Parser.parseDecPattern(): PDecPattern? = when (val token = current) {
    is Token.Ident -> DecPattern.Name(token.name).end()
    is Token.Parens -> parseComponents(token.tokens)
    is Token.Brackets -> parseDecObject(token.tokens)
    Token.Wildcard -> DecPattern.Wildcard.end()
    else -> null
}

fun Parser.requireDecPattern() = parseDecPattern() ?: run {
    err("Invalid pattern")
    DecPattern.Wildcard.noPos().also { advance() }
}

private fun Parser.parseComponentsBody(): List<PDecPattern> = recBuildList {
    if (current == Token.End) {
        return this
    } else {
        add(requireDecPattern())
        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }
}

private fun Parser.parseComponents(tokens: List<PToken>): PDecPattern =
    DecPattern.Components(parenthesizedOf(tokens).parseComponentsBody()).end()

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