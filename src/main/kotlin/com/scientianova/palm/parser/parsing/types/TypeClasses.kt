package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.data.types.TypeClass
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

private fun Parser.parseTCBody() = recBuildList<Item> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem(parseDecModifiers())?.let(this::add)
    }
}

fun Parser.parseTypeClass(modifiers: List<PDecMod>): TypeClass {
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = current.let { brace ->
        if (brace is Token.Braces) {
            scopedOf(brace.tokens).parseTCBody()
        } else {
            emptyList()
        }
    }

    return TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}