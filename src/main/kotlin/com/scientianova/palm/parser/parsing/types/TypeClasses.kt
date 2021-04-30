package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.parser.parsing.top.registerParsedItem
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.superItems
import com.scientianova.palm.util.recBuildList

private fun Parser.parseTCBody(id: ItemId) = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem()?.let { add(it); superItems[id] = it }
    }
}

fun Parser.parseTypeClass(modifiers: List<PDecMod>) = registerParsedItem { id ->
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = current.let { brace ->
        if (brace is Token.Braces) {
            scopedOf(brace.tokens).parseTCBody(id)
        } else {
            emptyList()
        }
    }

    ItemKind.TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}