package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.missingFunParams
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseBinOps
import com.scientianova.palm.parser.parsing.expressions.parseScopeExpr
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireTypeAnn
import com.scientianova.palm.parser.parsing.types.parseTypeParams
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString

fun parseWhere(parser: Parser, constraints: MutableList<Pair<PString, PType>>) {
    if (parser.current != Token.Where) return
    parser.advance()

    recBuildList(constraints) {
        val name = parseIdent(parser)
        val type = requireTypeAnn(parser)
        add(name to type)

        if (parser.current == Token.Comma) {
            parser.advance()
        } else {
            return
        }
    }
}

fun parseFunParam(parser: Parser): FunParam = TODO()

private fun parseFunParamsBody(parser: Parser): List<FunParam> = recBuildList {
    if (parser.current == Token.RParen) {
        return this
    } else {
        add(parseFunParam(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

fun parseFunParams(parser: Parser): List<FunParam> {
    val marker = parser.mark()

    parser.advance()
    parser.trackNewline = false
    parser.excludeCurly = false

    val params = parseFunParamsBody(parser)

    parser.advance()
    marker.revertFlags()

    return params
}

fun parseFun(parser: Parser): Function {
    val constrains = mutableListOf<Pair<PString, PType>>()
    val typeParams = parseTypeParams(parser.advance(), constrains)
    val name = parseIdent(parser)

    if (parser.current != Token.RParen) parser.err(missingFunParams)
    val params = parseFunParams(parser)
    val type = parseTypeAnn(parser)

    parseWhere(parser, constrains)
    val expr = when (parser.current) {
        Token.Assign -> parseBinOps(parser.advance())
        Token.LBrace -> parseScopeExpr(parser)
        else -> null
    }

    return Function(name, TODO(), typeParams, constrains, params, type, expr)
}