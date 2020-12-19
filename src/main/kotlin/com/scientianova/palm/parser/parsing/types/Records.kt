package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.Record
import com.scientianova.palm.parser.data.types.RecordProperty
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqExpr
import com.scientianova.palm.parser.parsing.expressions.requireTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireTypeBinOps
import com.scientianova.palm.parser.parsing.top.parseParamModifiers
import com.scientianova.palm.parser.recBuildList

fun parseRecord(parser: Parser, modifiers: List<DecModifier>): Record {
    val name = parseIdent(parser)
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    parseWhere(parser, constraints)

    return when (parser.current) {
        Token.LBrace -> {
            val properties = parseRecordProperties(parser.advance())
            Record.Normal(name, modifiers, typeParams, constraints, properties)
        }
        Token.LParen -> {
            val components = parseTypeTuple(parser.advance())
            Record.Tuple(name, modifiers, typeParams, constraints, components)
        }
        else -> Record.Single(name, modifiers, typeParams, constraints)
    }
}

fun parseTypeTuple(parser: Parser): List<PType> = recBuildList {
    if (parser.current == Token.RParen) {
        parser.advance()
        return this
    } else {
        add(requireTypeBinOps(parser))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> {
                parser.advance()
                return this
            }
            else -> parser.err(unclosedParenthesis)
        }
    }
}

private fun parseRecordProperty(parser: Parser): RecordProperty {
    val modifiers = parseParamModifiers(parser)
    val name = parseIdent(parser)
    val type = requireTypeAnn(parser)
    val default = parseEqExpr(parser)
    return RecordProperty(name, modifiers, type, default)
}

private fun parseRecordProperties(parser: Parser): List<RecordProperty> = recBuildList {
    if (parser.current == Token.RBrace) {
        parser.advance()
        return this
    } else {
        add(parseRecordProperty(parser))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RBrace -> {
                parser.advance()
                return this
            }
            else -> parser.err(unclosedParenthesis)
        }
    }
}