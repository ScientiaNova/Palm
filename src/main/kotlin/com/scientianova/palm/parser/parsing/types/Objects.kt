package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseInitializer
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.parser.parsing.top.registerParsedItem
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.superItems
import com.scientianova.palm.util.recBuildList

fun Parser.parseObjectBody(id: ItemId) = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(parseInitializer())
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(parseInitializer())
                else -> parseItem(modifiers)?.let { add(it); superItems[id] = it }
            }
        }
    }
}

fun Parser.parseObject(modifiers: List<PDecMod>) = registerParsedItem { id ->
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty { parseObjectBody(id) }

    ItemKind.Object(name, modifiers, superTypes, body)
}