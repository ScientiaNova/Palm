package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.data.types.Interface
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

fun Parser.parseInterfaceBody() = recBuildList<Item> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem(parseDecModifiers())?.let(this::add)
    }
}

fun Parser.parseInterfaceSuperTypes(): List<PType> = if (current == Token.Colon) {
    recBuildList {
        add(requireType())
        if (current == Token.Comma) {
            advance()
        } else {
            return this
        }
    }
} else {
    emptyList()
}

fun Parser.parseInterface(modifiers: List<PDecMod>): Interface {
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = inParensOrEmpty { parseClassTypeParams(constraints) }
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = inBracesOrEmpty(Parser::parseInterfaceBody)

    return Interface(name, modifiers, typeParams, constraints, superTypes, body)
}