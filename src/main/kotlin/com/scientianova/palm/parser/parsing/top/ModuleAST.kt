package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.implIdent
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.data.top.ModuleAST
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.types.parseImpl
import com.scientianova.palm.util.recBuildList

fun Parser.parseFile(): ModuleAST {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    val items = parseItems()
    return ModuleAST(annotations, imports, items)
}

fun Parser.parseInitializer(): ItemKind = ItemKind.Initializer(advance().requireScope())

private fun Parser.parseItems() = recBuildList<ItemKind> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(parseInitializer())
        implIdent -> add(advance().parseImpl())
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(parseInitializer())
                else -> parseItem(modifiers)?.let(::add)
            }
        }
    }
}