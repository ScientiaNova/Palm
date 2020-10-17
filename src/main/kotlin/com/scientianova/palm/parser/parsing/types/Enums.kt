package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.Enum
import com.scientianova.palm.parser.data.types.EnumCase
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseAnnotations
import com.scientianova.palm.parser.recBuildList

fun parseEnum(parser: Parser, modifiers: List<DecModifier>): Enum {
    val name = parseIdent(parser)
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    parseWhere(parser, constraints)

    val cases = if (parser.current == Token.LBrace) {
        parseCases(parser)
    } else {
        emptyList()
    }

    return Enum(name, modifiers, typeParams, constraints, cases)
}

private fun parseCases(parser: Parser): List<EnumCase> = recBuildList {
    if (parser.current == Token.RBrace) {
        return this
    } else {
        add(parseCase(parser))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RBrace -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

private fun parseCase(parser: Parser): EnumCase {
    val annotations = parseAnnotations(parser)
    val name = parseIdent(parser)

    return when (parser.current) {
        Token.LBrace -> {
            val properties = parseRecordProperties(parser)
            EnumCase.Normal(name, annotations, properties)
        }
        Token.LParen -> {
            val components = parseTypeTuple(parser)
            EnumCase.Tuple(name, annotations, components)
        }
        else -> EnumCase.Single(name, annotations)
    }
}