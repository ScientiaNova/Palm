package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

private fun Parser.parseTCBody() = recBuildList<ItemKind> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem()?.let(::add)
    }
}

fun Parser.parseTypeClass(modifiers: List<PDecMod>): ItemKind {
    val name = parseIdent()
    val typeParams = parseTypeParams()
    val superTypes = parseInterfaceSuperTypes()
    val constraints = parseWhere()
    val body = current.let { brace ->
        if (brace is Token.Braces) {
            scopedOf(brace.tokens).parseTCBody()
        } else {
            emptyList()
        }
    }

    return ItemKind.TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}