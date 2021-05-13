package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Expr
import com.scientianova.palm.parser.data.expressions.PExpr
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.*
import com.scientianova.palm.parser.parsing.types.constraints
import com.scientianova.palm.parser.parsing.types.parseTypeParams
import com.scientianova.palm.parser.parsing.types.parseWhere
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

fun Parser.parseFun(modifiers: List<PDecMod>) = registerParsedItem {
    val constrains = constraints()
    val name = parseIdent()
    val typeParams = parseTypeParams(constrains)
    val context = parseContextParams()

    val params = inParensOr(Parser::parseFunParams) {
        err("Missing parameters")
        emptyList()
    }
    val type = parseTypeAnn()
    parseWhere(constrains)
    val expr = parseFunBody()

    ItemKind.Function(name, modifiers, typeParams, constrains, context, params, type, expr)
}

fun Parser.parseFunBody(): PExpr? = when (val token = current) {
    Token.Assign -> advance().parseBinOps()
    is Token.Braces -> parseScopeBody(token.tokens).map(Expr::Scope)
    else -> null
}

fun Parser.requireFunBody() = parseFunBody() ?: run {
    err("Missing function body")
    Expr.Error.end()
}