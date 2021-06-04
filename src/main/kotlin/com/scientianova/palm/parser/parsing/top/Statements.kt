package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PScope
import com.scientianova.palm.parser.data.expressions.Scope
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.parsing.types.*
import com.scientianova.palm.util.recBuildList

private fun Parser.parseStatement(): Statement {
    val startIndex = index
    val modifiers = parseDecModifiers()
    return when (current) {
        Token.Let -> advance().parseLet(modifiers)
        Token.Def -> advance().parseDef(modifiers)
        Token.Type -> when (advance().current) {
            Token.Class -> advance().parseTypeClass(modifiers)
            else -> parseTpeAlias(modifiers)
        }
        Token.Class -> advance().parseClass(modifiers)
        Token.Object -> advance().parseObject(modifiers)
        Token.Interface -> advance().parseInterface(modifiers)
        Token.Impl -> advance().parseImpl()
        Token.Constructor -> advance().parseConstructor(modifiers)
        else -> {
            index = startIndex
            when (current) {
                Token.Defer -> Statement.Defer(advance().requireScope())
                else -> Statement.Expr(requireExpr())
            }
        }
    }
}

private fun Parser.parseLet(modifiers: List<PDecMod>) = current.let { curr ->
    if (curr is Token.Ident) {
        when (next) {
            Token.Less, is Token.Parens, is Token.Brackets -> parseFun(modifiers, true, curr.name.end())
            else -> parseVarDec(modifiers)
        }
    } else parseVarDec(modifiers)
}

private fun Parser.parseDef(modifiers: List<PDecMod>) =
    if (current == Token.Mut) parseProperty(modifiers, true, advance().parseIdent())
    else {
        val name = parseIdent()
        when (current) {
            is Token.Brackets ->
                if (next is Token.Parens) parseFun(modifiers, false, name)
                else parseProperty(modifiers, false, name)
            Token.Less, is Token.Parens -> parseFun(modifiers, false, name)
            else -> parseProperty(modifiers, false, name)
        }
    }

private fun Parser.parseVarDec(modifiers: List<PDecMod>): Statement {
    val pattern = requireDecPattern()
    val type = parseTypeAnn()
    val expr = parseEqExpr()
    return Statement.Var(modifiers, pattern, type, expr)
}

fun Parser.parseStatements(): Scope = recBuildList {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            add(parseStatement())
            when (current) {
                Token.Semicolon -> advance()
                Token.End -> return this
                else -> {
                    if (!lastNewline) err("Missing new line or semicolon")
                }
            }
        }
    }
}

fun Parser.parseScopeBody(tokens: List<PToken>): PScope = scopedOf(tokens).parseStatements().end()

fun Parser.parseScope(): PScope? = current.let { braces ->
    if (braces is Token.Braces) {
        parseScopeBody(braces.tokens)
    } else {
        null
    }
}

fun Parser.requireScope() = current.let { braces ->
    if (braces is Token.Braces) {
        parseScopeBody(braces.tokens)
    } else {
        emptyList<Statement>().noPos()
    }
}