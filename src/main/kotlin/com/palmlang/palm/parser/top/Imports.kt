package com.palmlang.palm.parser.top

import com.palmlang.palm.ast.top.Import
import com.palmlang.palm.ast.top.ImportBody
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.PathType
import com.palmlang.palm.util.recBuildList
import com.palmlang.palm.util.recBuildListN

fun Parser.parseImport(): Import = when (current) {
    Token.Mod -> Import(PathType.Module, advance().parseImportPath(mutableListOf()))
    Token.SuMod -> Import(PathType.Super, advance().parseImportPath(mutableListOf()))
    else -> Import(PathType.Root, parseImportPath(mutableListOf(parseIdent())))
}

fun Parser.parseImportPath(path: MutableList<PString>): ImportBody = recBuildListN(path) {
    when (current) {
        Token.Dot -> when (val curr = advance().current) {
            is Token.Ident -> add(curr.name.end())
            is Token.Braces -> ImportBody.Group(path, parenthesizedOf(curr.tokens).parseImportGroup())
                .also { advance() }
            is Token.Asterisk -> ImportBody.Module(
                path,
                if (advance().current == Token.Backslash) advance().inParensOrEmpty { parseHideItems() } else emptyList())
            else -> {
                err("Dangling dot")
                advance()
                ImportBody.Item(path, null)
            }
        }
        Token.As -> ImportBody.Item(path, advance().parseIdent())
        else -> ImportBody.Item(path, null)
    }
}

fun Parser.parseHideItems(): List<PString> = recBuildList {
    if (current == Token.End) return this

    add(parseIdent())

    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
    }
}

private fun Parser.parseImportGroup(): List<ImportBody> = recBuildList {
    if (current == Token.End) return this

    add(when (current) {
        Token.Mod -> ImportBody.Item(emptyList(), if (advance().current == Token.As) advance().parseIdent() else null)
        else -> parseImportPath(mutableListOf(parseIdent()))
    })

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