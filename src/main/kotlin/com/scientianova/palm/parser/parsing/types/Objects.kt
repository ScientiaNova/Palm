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
import com.scientianova.palm.util.recBuildList

fun Parser.parseObjectBody() = recBuildList<ItemKind> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(parseInitializer())
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(parseInitializer())
                else -> parseItem(modifiers)?.let(::add)
            }
        }
    }
}

fun Parser.parseObject(modifiers: List<PDecMod>): ItemKind {
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty { parseObjectBody() }

    return ItemKind.Object(name, modifiers, superTypes, body)
}