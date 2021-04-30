package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.parser.parsing.top.registerParsedItem
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.superItems
import com.scientianova.palm.util.recBuildList

fun Parser.parseInterfaceBody(id: ItemId) = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem(parseDecModifiers())?.let { add(it); superItems[id] = it }
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

fun Parser.parseInterface(modifiers: List<PDecMod>) = registerParsedItem { id ->
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = inParensOrEmpty { parseClassTypeParams(constraints) }
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = inBracesOrEmpty { parseInterfaceBody(id) }

    ItemKind.Interface(name, modifiers, typeParams, constraints, superTypes, body)
}