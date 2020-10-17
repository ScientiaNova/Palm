package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.unclosedImportGroup
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.Import
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.parser.recBuildListN
import com.scientianova.palm.util.PString

private fun parseImport(parser: Parser): Import = recBuildListN<PString> {
    when (parser.current) {
        Token.Wildcard -> return Import.Package(this)
        Token.LBrace -> {
            val group = parseImportGroup(parser.advance())
            parser.advance()

            return Import.Group(this, group)
        }
        else -> add(parseIdent(parser))
    }
    add(parseIdent(parser))
    when (parser.current) {
        Token.Dot -> parser.advance()
        Token.As -> return Import.Regular(this, parseIdent(parser.advance()))
        else -> return Import.Regular(this, null)
    }
}

private fun parseImportGroup(parser: Parser): List<Import> = recBuildList {
    if (parser.current == Token.RBrace) {
        return this
    }

    add(
        if (parser.current == Token.This) {
            parser.advance()
            Import.Regular(emptyList(), null)
        } else {
            parseImport(parser)
        }
    )

    when (parser.current) {
        Token.Comma -> parser.advance()
        Token.RBrace -> return this
        else -> parser.err(unclosedImportGroup)
    }
}

fun parseImports(parser: Parser): List<Import> = recBuildList {
    when (parser.current) {
        Token.Semicolon -> parser.advance()
        Token.Import -> add(parseImport(parser))
        else -> return this
    }
}