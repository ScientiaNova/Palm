package com.palmlang.palm.parser.top

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.DecModifier
import com.palmlang.palm.ast.top.FunParam
import com.palmlang.palm.ast.top.OptionallyTypedFunParam
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.expressions.parseEqExpr
import com.palmlang.palm.parser.expressions.parseTypeAnn
import com.palmlang.palm.parser.expressions.requireDecPattern
import com.palmlang.palm.parser.expressions.requireTypeAnn
import com.palmlang.palm.parser.types.parseTypeParams
import com.palmlang.palm.parser.types.parseWhere
import com.palmlang.palm.util.map
import com.palmlang.palm.util.recBuildList


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

fun Parser.parseFun(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val typeParams = parseTypeParams()
    val context = parseContextParams()

    val params = inParensOr(Parser::parseFunParams) {
        err("Missing parameters")
        emptyList()
    }
    val type = parseTypeAnn()
    val constrains = parseWhere()
    val expr = parseEqExpr()

    return Statement.Function(name, modifiers, typeParams, constrains, context, params, type, expr)
}