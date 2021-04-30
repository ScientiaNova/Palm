package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseContextParams
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.parser.parsing.top.registerParsedItem
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.superItems
import com.scientianova.palm.util.recBuildList

private fun Parser.parseImplBody(id: ItemId) = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem()?.let { add(it); superItems[id] = it }
    }
}

fun Parser.parseImpl() = registerParsedItem { id ->
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val type = requireType()
    val context = parseContextParams()
    parseWhere(constraints)
    val body = inBracesOrEmpty { parseImplBody(id) }

    ItemKind.Implementation(type, typeParams, constraints, context, body)
}