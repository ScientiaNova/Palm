package com.palmlang.palm.parser.types

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.ImplementationCase
import com.palmlang.palm.ast.top.ImplementationKind
import com.palmlang.palm.parser.top.parseStatements
import com.palmlang.palm.parser.expressions.requireType
import com.palmlang.palm.parser.top.parseContextParams
import com.palmlang.palm.util.recBuildList

private fun Parser.parseImplCases() = recBuildList<ImplementationCase> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val context = parseContextParams()
            if (current == Token.Arrow) advance()
            else err("Missing arrow")
            val body = inBracesOr(Parser::parseStatements) {
                err("Missing case body")
                emptyList()
            }
            add(ImplementationCase(context, body))
        }
    }
}

fun Parser.parseImpl(): Statement {
    val typeParams = parseTypeParams()
    val type = requireType()
    val context = parseContextParams()
    val constraints = parseWhere()
    val kind = if (current == Token.When) {
        ImplementationKind.Cases(advance().inBracesOr(Parser::parseImplCases) {
            err("Missing cases")
            emptyList()
        })
    } else ImplementationKind.Single(inBracesOrEmpty { parseStatements() })

    return Statement.Implementation(type, typeParams, constraints, context, kind)
}