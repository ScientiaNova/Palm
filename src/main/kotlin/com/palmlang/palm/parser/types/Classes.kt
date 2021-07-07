package com.palmlang.palm.parser.types

import com.palmlang.palm.ast.expressions.Scope
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.*
import com.palmlang.palm.parser.expressions.parseCallArgs
import com.palmlang.palm.parser.expressions.parseEqExpr
import com.palmlang.palm.parser.expressions.requireType
import com.palmlang.palm.parser.expressions.requireTypeAnn
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.top.*
import com.palmlang.palm.util.at
import com.palmlang.palm.util.recBuildList

private fun Parser.parseSuperType(): PSuperType {
    val type = requireType()
    return when (val curr = current) {
        is Token.Parens -> {
            val args = parenthesizedOf(curr.tokens).parseCallArgs()
            SuperType.Class(type, args).end(type.start)
        }
        else -> SuperType.Interface(type).at(type.start, type.next)
    }
}

fun Parser.parseClassSuperTypes(): List<PSuperType> = if (current == Token.Colon) {
    advance()
    recBuildList {
        add(parseSuperType())
        if (current == Token.Comma) {
            advance()
        } else {
            return this
        }
    }
} else {
    emptyList()
}

private fun Parser.parsePrimaryParam(): PrimaryParam {
    val modifiers = parseParamModifiers()
    val decHandling =
        if (current == Token.Let)
            if (advance().current == Token.Mut) DecHandling.Mut.also { advance() }
            else DecHandling.Umm
        else DecHandling.None
    val name = parseIdent()
    val type = requireTypeAnn()
    val default = parseEqExpr()
    return PrimaryParam(modifiers, decHandling, name, type, default)
}

private fun Parser.parsePrimaryParams() =
    recBuildList<PrimaryParam> {
        if (current == Token.End) {
            return this
        }

        add(parsePrimaryParam())

        when (current) {
            Token.Comma -> advance()
            Token.End -> return this
            else -> err("Unclosed parentheses")
        }
    }


fun Parser.parseClass(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()

    val typeParams = parseTypeParams()

    val constructorModifiers = parseDecModifiers()
    val atConstructor = current == Token.Constructor

    if (!(constructorModifiers.isEmpty() || atConstructor)) {
        err("Missing constructor")
    }

    if (atConstructor) {
        advance()
    }

    val primaryConstructor = inParensOr(Parser::parsePrimaryParams) { null }
    val superTypes = parseClassSuperTypes()
    val constraints = parseWhere()
    val body = parseScope(name)

    return Statement.Class(
        name,
        modifiers,
        constructorModifiers,
        primaryConstructor,
        typeParams,
        constraints,
        superTypes,
        body
    )
}

fun Parser.parseConstructor(modifiers: List<PDecMod>): Statement {
    val params = inParensOr(Parser::parseFunParams) {
        err("Missing params")
        emptyList()
    }

    val primaryCall = if (advance().current == Token.Colon) {
        if (advance().current !is Token.Parens) {
            err("Expected (")
        }

        advance().parseCallArgs().also { advance() }
    } else {
        null
    }

    val body = when (val braces = current) {
        is Token.Braces ->
            Scope(null, null, scopedOf(braces.tokens).parseStatements()).end()
        else -> null
    }

    return Statement.Constructor(modifiers, params, primaryCall, body)
}