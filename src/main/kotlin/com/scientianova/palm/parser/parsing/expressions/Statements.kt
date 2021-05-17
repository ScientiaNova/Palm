package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.ExprScope
import com.scientianova.palm.parser.data.expressions.PExprScope
import com.scientianova.palm.parser.data.expressions.ScopeStmt
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.util.recBuildList

private fun Parser.parseStatement(): ScopeStmt {
    val startIndex = index
    val modifiers = parseDecModifiers()
    return when (current) {
        Token.Val -> parseVarDec(modifiers, false)
        Token.Var -> parseVarDec(modifiers, true)
        else -> {
            index = startIndex
            when (current) {
                Token.Defer -> ScopeStmt.Defer(advance().requireScope())
                else -> ScopeStmt.Expr(requireExpr())
            }
        }
    }
}

private fun Parser.parseVarDec(modifiers: List<PDecMod>, mutable: Boolean): ScopeStmt {
    val pattern = advance().requireDecPattern()
    val type = parseTypeAnn()
    val expr = parseEqExpr()
    return ScopeStmt.Dec(modifiers, mutable, pattern, type, expr)
}

fun Parser.parseStatements(): ExprScope = recBuildList {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            add(parseStatement())
            val sep = current
            when {
                sep == Token.Semicolon -> advance()
                sep == Token.End -> return this
                lastNewline -> Unit
                else -> err("Missing new line or semicolon")
            }
        }
    }
}

fun Parser.parseScopeBody(tokens: List<PToken>): PExprScope = scopedOf(tokens).parseStatements().end()

fun Parser.parseScope(): PExprScope? = current.let { braces ->
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
        emptyList<ScopeStmt>().noPos()
    }
}