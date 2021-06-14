package com.palmlang.palm.parser.top

import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.expressions.requireExpr
import com.palmlang.palm.parser.types.*
import com.palmlang.palm.util.recBuildList

private fun Parser.parseStatement(): Statement {
    val startIndex = index
    val modifiers = parseDecModifiers()
    return when (current) {
        Token.Let -> advance().parseProperty(modifiers)
        Token.Def -> advance().parseFun(modifiers)
        Token.Type -> advance().parseTpeAlias(modifiers)
        Token.Class -> advance().parseClass(modifiers)
        Token.Object -> advance().parseObject(modifiers)
        Token.Interface -> advance().parseInterface(modifiers)
        Token.Impl -> advance().parseImpl()
        Token.Constructor -> advance().parseConstructor(modifiers)
        else -> {
            index = startIndex
            Statement.Expr(requireExpr())
        }
    }
}


fun Parser.parseStatements(): List<Statement> = recBuildList {
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