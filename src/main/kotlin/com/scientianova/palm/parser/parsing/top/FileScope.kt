package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.implIdent
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.FileScope
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.types.parseImpl
import com.scientianova.palm.queries.FileId
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.fileIdToParsed
import com.scientianova.palm.queries.fileToItems
import com.scientianova.palm.util.recBuildList

fun Parser.parseFile() = FileId().also {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    fileToItems[it] = parseStatements()
    fileIdToParsed[it] = FileScope(annotations, imports)
}

fun Parser.parseInitializer(): ItemId = registerParsedItem {
    ItemKind.Initializer(advance().requireScope())
}

private fun Parser.parseStatements() = recBuildList<ItemId> {
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