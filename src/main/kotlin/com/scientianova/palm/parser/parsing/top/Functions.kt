package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.doubleInlineHandling
import com.scientianova.palm.errors.missingFunParams
import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.Function
import com.scientianova.palm.parser.data.top.InlineHandling
import com.scientianova.palm.parser.data.top.ParamInfo
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.*
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

class ParamInfoBuilder {
    var using = false
    var given = false
    var inlineHandling: InlineHandling? = null

    fun build() = ParamInfo(using, given, inlineHandling ?: InlineHandling.None)
}

fun parseParamModifiers(parser: Parser, builder: ParamInfoBuilder): ParamInfo {
    when (parser.current) {
        Token.Using -> builder.using = true
        Token.Given -> builder.given = true
        Token.Noinline -> {
            if (builder.inlineHandling != null) parser.err(doubleInlineHandling)
            builder.inlineHandling = InlineHandling.NoInline
        }
        Token.Crossinline -> {
            if (builder.inlineHandling != null) parser.err(doubleInlineHandling)
            builder.inlineHandling = InlineHandling.CrossInline
        }
        else -> return builder.build()
    }
    return parseParamModifiers(parser.advance(), builder)
}

fun parseFunParam(parser: Parser): FunParam {
    val info = parseParamModifiers(parser, ParamInfoBuilder())
    val pattern = requireDecPattern(parser)
    val type = requireTypeAnn(parser)
    val default = parseEqExpr(parser)
    return FunParam(info, pattern, type, default)
}

private fun parseFunParamsBody(parser: Parser): List<FunParam> = recBuildList<FunParam> {
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
    val marker = parser.Marker()

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

    return Function(name, typeParams, constrains, params, type, expr)
}