package com.palmlang.palm.parser.types

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.lexer.inIdent
import com.palmlang.palm.lexer.outIdent
import com.palmlang.palm.lexer.whereIdent
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.PType
import com.palmlang.palm.ast.expressions.VarianceMod
import com.palmlang.palm.ast.top.PTypeParam
import com.palmlang.palm.ast.top.TypeParam
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.expressions.requireType
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.recBuildList

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