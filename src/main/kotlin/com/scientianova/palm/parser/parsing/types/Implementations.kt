package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.parser.data.types.Implementation
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseContextParams
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

private fun Parser.parseImplBody() = recBuildList<Item> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem(parseDecModifiers())?.let(this::add)
    }
}

fun Parser.parseImpl(): Implementation {
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val type = requireType()
    val context = parseContextParams()
    parseWhere(constraints)
    val body = inBracesOrEmpty(Parser::parseImplBody)

    return Implementation(type, typeParams, constraints, context, body)
}