package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.typeIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Item
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parsing.types.*
import com.scientianova.palm.queries.ItemId

fun Parser.parseItemKind(modifiers: List<PDecMod> = parseDecModifiers()): ItemKind? = when (current) {
    Token.Fun -> ItemKind.Fun(parseFun(modifiers))
    Token.Val -> ItemKind.Prop(parseProperty(modifiers, false))
    Token.Var -> ItemKind.Prop(parseProperty(modifiers, true))
    typeIdent -> when (advance().current) {
        Token.Class -> ItemKind.TC(advance().parseTypeClass(modifiers))
        else -> ItemKind.Alias(parseTpeAlias(modifiers))
    }
    Token.Class -> ItemKind.Clazz(advance().parseClass(modifiers))
    Token.Object -> ItemKind.Obj(advance().parseObject(modifiers))
    Token.Interface -> ItemKind.Inter(advance().parseInterface(modifiers))
    Token.End -> {
        err("Expected declaration")
        null
    }
    else -> {
        err("Expected declaration").advance()
        null
    }
}

fun Parser.parseItem(modifiers: List<PDecMod>): Item? = parseItemKind()?.let { Item(ItemId(), it) }