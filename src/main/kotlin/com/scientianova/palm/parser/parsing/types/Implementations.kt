package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.ImplementationCase
import com.scientianova.palm.parser.data.top.ImplementationKind
import com.scientianova.palm.parser.data.top.ItemKind
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseContextParams
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

private fun Parser.parseImplBody() = recBuildList<ItemKind> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> parseItem()?.let(::add)
    }
}

private fun Parser.parseImplCases() = recBuildList<ImplementationCase> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val context = parseContextParams()
            if (current == Token.Arrow) advance()
            else err("Missing arrow")
            val body = inBracesOr(Parser::parseImplBody) {
                err("MIssing case body")
                emptyList()
            }
        }
    }
}

fun Parser.parseImpl(): ItemKind {
    val typeParams = parseTypeParams()
    val type = requireType()
    val context = parseContextParams()
    val constraints = parseWhere()
    val kind = if (current == Token.When) {
        ImplementationKind.Cases(advance().inBracesOr(Parser::parseImplCases) {
            err("Missing cases")
            emptyList()
        })
    } else ImplementationKind.Single(inBracesOrEmpty { parseImplBody() })

    return ItemKind.Implementation(type, typeParams, constraints, context, kind)
}