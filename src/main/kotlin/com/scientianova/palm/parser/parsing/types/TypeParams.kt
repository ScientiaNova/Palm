package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.inIdent
import com.scientianova.palm.lexer.outIdent
import com.scientianova.palm.lexer.whereIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.expressions.VarianceMod
import com.scientianova.palm.parser.data.top.PTypeParam
import com.scientianova.palm.parser.data.top.TypeParam
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

fun Parser.parseTypeParams(): List<PTypeParam> = if (current == Token.Less) {
    advance()
    recBuildList {
        if (current == Token.Greater) {
            advance()
            return this
        }

        val start = pos
        val variance = when (current) {
            inIdent -> {
                advance()
                VarianceMod.In
            }
            outIdent -> {
                advance()
                VarianceMod.Out
            }
            else -> VarianceMod.None
        }

        val param = parseIdent()
        add(TypeParam(param, variance, if (current == Token.Colon) advance().parseTypeBound() else emptyList()).end(start))

        when (current) {
            Token.Comma -> advance()
            Token.Greater -> {
                advance()
                return this
            }
            else -> err("Unclosed angle bracket")
        }
    }
} else emptyList()

fun Parser.parseWhere(): List<Pair<PString, List<PType>>> {
    if (current === whereIdent) advance() else return emptyList()

    return recBuildList {
        val name = parseIdent()
        if (current == Token.Colon) advance() else err("Missing colon")
        val type = parseTypeBound()
        add(name to type)

        if (current == Token.Comma) {
            advance()
        } else {
            return this
        }
    }
}

fun Parser.parseTypeBound(): List<PType> = recBuildList {
    add(requireType())
    if (current == Token.And) {
        advance()
    } else {
        return this
    }
}