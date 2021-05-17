package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.typeIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parsing.types.*

fun Parser.parseItem(modifiers: List<PDecMod> = parseDecModifiers()): ItemKind? = when (current) {
    Token.Fun -> advance().parseFun(modifiers)
    Token.Val -> advance().parseProperty(modifiers, false)
    Token.Var -> advance().parseProperty(modifiers, true)
    typeIdent -> when (advance().current) {
        Token.Class -> advance().parseTypeClass(modifiers)
        else -> parseTpeAlias(modifiers)
    }
    Token.Class -> advance().parseClass(modifiers)
    Token.Object -> advance().parseObject(modifiers)
    Token.Interface -> advance().parseInterface(modifiers)
    Token.End -> {
        err("Expected declaration")
        null
    }
    else -> {
        err("Expected declaration").advance()
        null
    }
}