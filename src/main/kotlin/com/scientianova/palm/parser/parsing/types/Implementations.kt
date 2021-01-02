package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.typeIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.types.ImplStmt
import com.scientianova.palm.parser.data.types.Implementation
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireEqType
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseContextParams
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.util.recBuildList

private fun Parser.parseImplBody() = recBuildList<ImplStmt> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                Token.Val -> add(ImplStmt.Property(advance().parseProperty(modifiers, false)))
                Token.Var -> add(ImplStmt.Property(advance().parseProperty(modifiers, true)))
                Token.Fun -> add(ImplStmt.Method(advance().parseFun(modifiers)))
                typeIdent -> add(parseAssociatedType())
                else -> err("Expected implementation member").also { advance() }
            }
        }
    }
}

private fun Parser.parseAssociatedType(): ImplStmt =
    ImplStmt.AssociatedType(parseIdent(), requireEqType())

fun Parser.parseImpl(): Implementation {
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val type = requireType()
    val context = parseContextParams()
    parseWhere(constraints)
    val body = inBracesOrEmpty(Parser::parseImplBody)

    return Implementation(type, typeParams, constraints, context, body)
}