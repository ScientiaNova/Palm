package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.implIdent
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.lexer.typeIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.data.types.TypeDec
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireEqType
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.types.*
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
        implIdent -> add(FileStmt.Impl(advance().parseImpl()))
        initIdent -> add(FileStmt.Init(advance().requireScope()))
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                Token.Val -> add(FileStmt.Prop(advance().parseProperty(modifiers, false)))
                Token.Var -> add(FileStmt.Prop(advance().parseProperty(modifiers, true)))
                Token.Fun -> add(FileStmt.Fun(advance().parseFun(modifiers)))
                typeIdent -> add(when (advance().current) {
                    Token.Class -> FileStmt.TC(advance().parseTypeClass(modifiers))
                    else -> parseTpeAlias(modifiers)
                })
                else -> parseTypeDec(modifiers)?.let { add(FileStmt.Type(it)) }
            }
        }
    }
}

fun Parser.parseTypeDec(modifiers: List<DecModifier>): TypeDec? = when (current) {
    Token.Class -> advance().parseClass(modifiers)
    Token.Object -> advance().parseObject(modifiers)
    Token.Interface -> advance().parseInterface(modifiers)
    Token.End -> {
        err("Expected type declaration")
        null
    }
    else -> {
        err("Expected type declaration").advance()
        null
    }
}

private fun Parser.parseTpeAlias(modifiers: List<DecModifier>): FileStmt {
    val name = parseIdent()
    val params = if (current == Token.Less) {
        advance().parseAliasParams()
    } else {
        emptyList()
    }

    val actual = requireEqType()

    return FileStmt.TypeAlias(name, modifiers, params, actual)
}

private fun Parser.parseAliasParams() = recBuildList<PString> {
    if (current == Token.Greater) {
        advance()
        return this
    }

    add(parseIdent())

    when (current) {
        Token.Comma -> advance()
        Token.Greater -> {
            advance()
            return this
        }
        else -> err("Unclosed type params")
    }
}