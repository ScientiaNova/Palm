package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.missingExpression
import com.scientianova.palm.errors.missingFunParams
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.OptionallyTypedFunParam
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.*
import com.scientianova.palm.parser.parsing.types.constraints
import com.scientianova.palm.parser.parsing.types.parseTypeParams
import com.scientianova.palm.parser.parsing.types.parseWhere
import com.scientianova.palm.parser.recBuildList

fun parseParamModifiers(parser: Parser) = recBuildList<DecModifier> {
    val current = parser.current
    if (current == Token.At) {
        add(DecModifier.Annotation(parseAnnotation(parser)))
    } else {
        val modifier = current.decModifier ?: return this
        val currentMark = parser.mark()
        when (parser.advance().current) {
            Token.Colon, Token.RParen, Token.Comma, Token.Assign -> {
                currentMark.revertIndex()
                return this
            }
            else -> add(modifier)
        }
    }
}

fun parseFunParam(parser: Parser): FunParam {
    val modifiers = parseParamModifiers(parser)
    val pattern = requireDecPattern(parser)
    val type = requireTypeAnn(parser)
    val default = parseEqExpr(parser)
    return FunParam(modifiers, pattern, type, default)
}

fun parseOptionallyTypedFunParam(parser: Parser): OptionallyTypedFunParam {
    val modifiers = parseParamModifiers(parser)
    val pattern = requireDecPattern(parser)
    val type = parseTypeAnn(parser)
    val default = parseEqExpr(parser)
    return OptionallyTypedFunParam(modifiers, pattern, type, default)
}

fun parseFunParams(parser: Parser): List<FunParam> = parser.withFlags(trackNewline = false, excludeCurly = false) {
    recBuildList {
        if (parser.current == Token.RParen) {
            parser.advance()
            return@withFlags this
        } else {
            add(parseFunParam(parser))
            when (parser.current) {
                Token.Comma -> parser.advance()
                Token.RParen -> {
                    parser.advance()
                    return@withFlags this
                }
                else -> parser.err(unclosedParenthesis)
            }
        }
    }
}

fun parseFun(parser: Parser, modifiers: List<DecModifier>): Function {
    val constrains = constraints()
    val typeParams = parseTypeParams(parser, constrains)
    val name = parseIdent(parser)

    if (parser.current != Token.LParen) {
        parser.err(missingFunParams)
    }

    val params = parseFunParams(parser.advance())
    val type = parseTypeAnn(parser)
    parseWhere(parser, constrains)
    val expr = parseFunBody(parser)

    return Function(name, modifiers, typeParams, constrains, params, type, expr)
}

fun parseFunBody(parser: Parser) = when (parser.current) {
    Token.Assign -> parseBinOps(parser.advance())
    Token.LBrace -> parseScopeExpr(parser)
    else -> null
}

fun requireFunBody(parser: Parser) = parseFunBody(parser) ?: parser.err(missingExpression)