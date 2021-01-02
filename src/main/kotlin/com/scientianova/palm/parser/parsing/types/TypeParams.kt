package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.whereIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

typealias MutableConstraints = MutableList<Pair<PString, List<PType>>>

fun constraints(): MutableConstraints = mutableListOf()

fun Parser.parseTypeParams(constraints: MutableConstraints): List<PString> = if (current == Token.Less) {
    advance()
    recBuildList {
        if (current == Token.Greater) {
            advance()
            return this
        }

        val param = parseIdent()
        if (current == Token.Colon) constraints.add(param to advance().parseTypeBound())
        add(param)

        when (current) {
            Token.Comma -> advance()
            Token.Greater -> {
                advance()
                return this
            }
            else -> err("Unclosed angle bracket")
        }
    }
} else {
    emptyList()
}

fun Parser.parseWhere(constraints: MutableList<Pair<PString, List<PType>>>) {
    if (current === whereIdent) advance() else return

    recBuildList(constraints) {
        val name = parseIdent()
        if (current == Token.Colon) advance() else err("Missing colon")
        val type = parseTypeBound()
        add(name to type)

        if (current == Token.Comma) {
            advance()
        } else {
            return
        }
    }
}

fun Parser.parseTypeBound(): List<PType> = recBuildList {
    add(requireType())
    if (current == Token.Plus) {
        advance()
    } else {
        return this
    }
}