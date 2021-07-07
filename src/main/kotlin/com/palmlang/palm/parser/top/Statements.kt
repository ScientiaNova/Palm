package com.palmlang.palm.parser.top

import com.palmlang.palm.ast.expressions.PScope
import com.palmlang.palm.ast.expressions.PStatement
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.expressions.parseScope
import com.palmlang.palm.parser.expressions.requireExpr
import com.palmlang.palm.parser.types.*
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.at
import com.palmlang.palm.util.recBuildList

private fun Parser.parseStatement(): PStatement {
    val startIndex = index
    val modifiers = parseDecModifiers()
    return when (current) {
        Token.Let -> advance().parseProperty(modifiers).at(startIndex, lastPos)
        Token.Def -> advance().parseFun(modifiers).at(startIndex, lastPos)
        Token.Type -> advance().parseTpeAlias(modifiers).at(startIndex, lastPos)
        Token.Class -> advance().parseClass(modifiers).at(startIndex, lastPos)
        Token.Object -> advance().parseObject(modifiers).at(startIndex, lastPos)
        Token.Interface -> advance().parseInterface(modifiers).at(startIndex, lastPos)
        Token.Impl -> advance().parseImpl().at(startIndex, lastPos)
        Token.Constructor -> advance().parseConstructor(modifiers).at(startIndex, lastPos)
        else -> {
            index = startIndex
            requireExpr()
        }
    }
}


fun Parser.parseStatements(): List<PStatement> = recBuildList {
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

fun Parser.parseScope(label: PString? = null): PScope? = current.let {
    if (it is Token.Braces) parseScope(null, it.tokens) else null
}