package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.FileScope
import com.scientianova.palm.parser.data.top.FileStmt
import com.scientianova.palm.parser.data.types.TypeAlias
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqType
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.types.parseTypeBound
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList

fun Parser.parseFile(): FileScope {
    val annotations = parseFileAnnotations()
    val imports = parseImports()
    val statements = parseStatements()

    return FileScope(annotations, imports, statements)
}

private fun Parser.parseStatements() = recBuildList<FileStmt> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(FileStmt.Init(advance().requireScope()))
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(FileStmt.Init(advance().requireScope()))
                else -> parseItem(modifiers)?.let { add(FileStmt.Item(it)) }
            }
        }
    }
}