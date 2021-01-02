package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.whereIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireTypeAnn
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

typealias Constraints = List<Pair<PString, PType>>
typealias MutableConstraints = MutableList<Pair<PString, PType>>

fun constraints(): MutableConstraints = mutableListOf()

fun Parser.parseTypeParams(constraints: MutableConstraints): List<PString> = if (current == Token.Less) {
    advance()
    recBuildList {
        if (current == Token.Greater) {
            advance()
            return this
        }

        val param = parseIdent()
        parseTypeAnn()?.let { constraints.add(param to it) }
        add(param)

        when (current) {
            Token.Comma -> advance()
            Token.End -> {
                advance()
                return this
            }
            else -> err("Unclosed angle bracket")
        }
    }
} else {
    emptyList()
}

fun Parser.parseWhere(constraints: MutableList<Pair<PString, PType>>) {
    if (current !== whereIdent) {
        return
    }

    advance()

    recBuildList(constraints) {
        val name = parseIdent()
        val type = requireTypeAnn()
        add(name to type)

        if (current == Token.Comma) {
            advance()
        } else {
            return
        }
    }
}