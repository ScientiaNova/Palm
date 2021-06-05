package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PScope
import com.scientianova.palm.parser.data.expressions.Scope
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.parsing.expressions.requireExpr
import com.scientianova.palm.parser.parsing.types.*
import com.scientianova.palm.util.recBuildList

private fun Parser.parseStatement(): Statement {
    val startIndex = index
    val modifiers = parseDecModifiers()
    return when (current) {
        Token.Let -> advance().parseProperty(modifiers)
        Token.Def -> advance().parseFun(modifiers)
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