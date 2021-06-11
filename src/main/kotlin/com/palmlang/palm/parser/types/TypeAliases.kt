package com.palmlang.palm.parser.types

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.expressions.parseEqType
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.recBuildList

fun Parser.parseTpeAlias(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val params = if (current == Token.Less) {
        advance().parseAliasParams()
    } else {
        emptyList()
    }
    val bound = if (current == Token.Colon) advance().parseTypeBound() else emptyList()
    val actual = parseEqType()

    return Statement.TypeAlias(name, modifiers, params, bound, actual)
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