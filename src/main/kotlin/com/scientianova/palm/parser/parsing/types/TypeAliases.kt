package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.data.types.TypeAlias
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

fun Parser.parseTpeAlias(modifiers: List<PDecMod>): TypeAlias {
    val name = parseIdent()
    val params = if (current == Token.Less) {
        advance().parseAliasParams()
    } else {
        emptyList()
    }
    val bound = if (current == Token.Colon) advance().parseTypeBound() else emptyList()
    val actual = parseEqType()

    return TypeAlias(name, modifiers, params, bound, actual)
}

private fun Parser.parseAliasParams() = recBuildList<PString> {
    if (current == Token.Greater) {
        advance()
        return this
    }

    add(parseIdent())

    when (current) {
        Token.Comma -> advance()
        Token.Greater -> {
            advance()
            return this
        }
        else -> err("Unclosed type params")
    }
}