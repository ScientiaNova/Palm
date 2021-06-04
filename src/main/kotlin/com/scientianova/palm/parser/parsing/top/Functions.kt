package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.FunParam
import com.scientianova.palm.parser.data.top.OptionallyTypedFunParam
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parsing.expressions.parseEqExpr
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireDecPattern
import com.scientianova.palm.parser.parsing.expressions.requireTypeAnn
import com.scientianova.palm.parser.parsing.types.parseTypeParams
import com.scientianova.palm.parser.parsing.types.parseWhere
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.map
import com.scientianova.palm.util.recBuildList


fun Parser.parseParamModifiers() = recBuildList<PDecMod> {
    when (val token = current) {
        Token.At -> parseAnnotation()?.let { add(it.map(DecModifier::Annotation)) }
        is Token.Ident -> if (token.backticked) return this else when (next) {
            Token.Colon, Token.End, Token.Comma, Token.Assign -> return this
            else -> add(identToDecMod(token.name) ?: return this)
        }
        else -> return this
    }
}

fun Parser.parseFunParam(): FunParam {
    val modifiers = parseParamModifiers()
    val pattern = requireDecPattern()
    val type = requireTypeAnn()
    val default = parseEqExpr()
    return FunParam(modifiers, pattern, type, default)
}

fun Parser.parseOptionallyTypedFunParam(): OptionallyTypedFunParam {
    val modifiers = parseParamModifiers()
    val pattern = requireDecPattern()
    val type = parseTypeAnn()
    val default = parseEqExpr()
    return OptionallyTypedFunParam(modifiers, pattern, type, default)
}

fun Parser.parseFunParams(): List<FunParam> =
    recBuildList {
        if (current == Token.End)
            return this

        add(parseFunParam())
        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Missing comma")
        }
    }

fun Parser.parseContextParams(): List<FunParam> = inBracketsOrEmpty(Parser::parseFunParams)

fun Parser.parseFun(modifiers: List<PDecMod>, local: Boolean, name: PString): Statement {
    val typeParams = parseTypeParams()
    val context = parseContextParams()

    val params = inParensOr(Parser::parseFunParams) {
        err("Missing parameters")
        emptyList()
    }
    val type = parseTypeAnn()
    val constrains = parseWhere()
    val expr = parseEqExpr()

    return Statement.Function(local, name, modifiers, typeParams, constrains, context, params, type, expr)
}