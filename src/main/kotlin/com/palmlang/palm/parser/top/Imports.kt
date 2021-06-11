package com.palmlang.palm.parser.top

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.lexer.hideIdent
import com.palmlang.palm.lexer.showIdent
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.top.Import
import com.palmlang.palm.ast.top.ImportBody
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.PathType
import com.palmlang.palm.util.recBuildList
import com.palmlang.palm.util.recBuildListN

fun Parser.parseImport(): Import = when (current) {
    Token.Mod -> Import(PathType.Module, advance().parseImportPath(mutableListOf()))
    Token.Super -> Import(PathType.Super, advance().parseImportPath(mutableListOf()))
    else -> Import(PathType.Root, parseImportPath(mutableListOf(parseIdent())))
}

fun Parser.parseImportPath(path: MutableList<PString>): ImportBody = recBuildListN(path) {
    when (current) {
        Token.Dot -> when (val curr = advance().current) {
            is Token.Ident -> add(curr.name.end())
            is Token.Braces -> ImportBody.Group(path, parenthesizedOf(curr.tokens).parseImportGroup())
                .also { advance() }
            else -> {
                err("Dangling dot")
                advance()
                ImportBody.File(path)
            }
        }
        Token.As -> ImportBody.Qualified(path, parseIdent())
        showIdent -> ImportBody.Show(path, when (val item = advance().current) {
            is Token.Braces -> parenthesizedOf(item.tokens).parseShowItems().also { advance() }
            else -> listOf(parseShowItem())
        })
        hideIdent -> ImportBody.Hide(path, when (val item = advance().current) {
            is Token.Braces -> parenthesizedOf(item.tokens).parseHideItems().also { advance() }
            else -> listOf(parseIdent())
        })
        else -> ImportBody.File(path)
    }
}

fun Parser.parseShowItem(): Pair<PString, PString?> =
    parseIdent() to if (current == Token.As) advance().parseIdent() else null

fun Parser.parseShowItems(): List<Pair<PString, PString?>> = recBuildList {
    if (current == Token.End) return this

    add(parseShowItem())

    when (current) {
        Token.Comma -> advance()
        Token.End -> return this
        else -> err("Missing comma")
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
        Token.Mod -> ImportBody.File(emptyList()).also { advance() }
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