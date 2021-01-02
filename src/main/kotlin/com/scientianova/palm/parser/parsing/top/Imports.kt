package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Import
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.recBuildList
import com.scientianova.palm.util.recBuildListN

fun Parser.parseImport(): Import = recBuildListN<PString> {
    when (val curr = current) {
        Token.Times -> return Import.Package(this).also { advance() }
        is Token.Braces ->
            return Import.Group(this, scopedOf(curr.tokens).parseImportGroup()).also { advance() }
        else -> add(parseIdent())
    }
    when (current) {
        Token.Dot -> advance()
        Token.As -> return Import.Regular(this, advance().parseIdent())
        else -> return Import.Regular(this, null)
    }
}

private fun Parser.parseImportGroup(): List<Import> = recBuildList {
    if (current == Token.End) return this

    add(parseImport())

    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

fun Parser.parseImports(): List<Import> = recBuildList {
    when (current) {
        Token.Semicolon -> advance()
        Token.Import -> add(parseImport())
        else -> return this
    }
}