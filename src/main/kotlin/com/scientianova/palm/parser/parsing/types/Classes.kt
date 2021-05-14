package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.lexer.outIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.VarianceMod
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.*
import com.scientianova.palm.parser.parsing.top.*
import com.scientianova.palm.queries.ItemId
import com.scientianova.palm.queries.superItems
import com.scientianova.palm.util.at
import com.scientianova.palm.util.recBuildList

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
    val decHandling = when (current) {
        Token.Val -> {
            advance()
            DecHandling.Val
        }
        Token.Var -> {
            advance()
            DecHandling.Var
        }
        else -> DecHandling.None
    }
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


fun Parser.parseClass(modifiers: List<PDecMod>) = registerParsedItem { id ->
    val name = parseIdent()

    val constraints = constraints()
    val typeParams = parseClassTypeParams(constraints)

    val constructorModifiers = parseDecModifiers()
    val atConstructor = current === initIdent

    if (!(constructorModifiers.isEmpty() || atConstructor)) {
        err("Missing constructor")
    }

    if (atConstructor) {
        advance()
    }

    val primaryConstructor = inParensOr(Parser::parsePrimaryParams) { null }

    val superTypes = parseClassSuperTypes()

    parseWhere(constraints)

    val body = inBracesOrEmpty { parseClassBody(id) }

    ItemKind.Class(
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

fun Parser.parseClassTypeParams(constraints: MutableConstraints): List<PClassTypeParam> = if (current == Token.Less) {
    advance()
    recBuildList {
        if (current == Token.Greater) {
            advance()
            return this
        }

        val start = pos
        val variance = when (current) {
            Token.In -> {
                advance()
                VarianceMod.In
            }
            outIdent -> {
                advance()
                VarianceMod.Out
            }
            else -> VarianceMod.None
        }

        val param = parseIdent()
        if (current == Token.Colon) constraints.add(param to advance().parseTypeBound())
        add(ClassTypeParam(param, variance).end(start))

        when (current) {
            Token.Comma -> advance()
            Token.Greater -> {
                advance()
                return this
            }
            else -> err("Unclosed angle bracket")
        }
    }
} else emptyList()

private fun Parser.parseClassBody(id: ItemId) = recBuildList<ItemId> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> when (val next = advance().current) {
                    is Token.Braces -> parseInitializer().let { add(it); superItems[id] = it }
                    is Token.Parens -> parseConstructor(next.tokens, modifiers).let { add(it); superItems[id] = it }
                    else -> err("Missing parameters")
                }
                else -> parseItem(modifiers)?.let { add(it); superItems[id] = it }
            }
        }
    }
}

private fun Parser.parseConstructor(paramTokens: List<PToken>, modifiers: List<PDecMod>) = registerParsedItem {
    val params = parenthesizedOf(paramTokens).parseFunParams()

    val primaryCall = if (advance().current == Token.Colon) {
        if (current != initIdent) {
            err("Missing `init`")
        }

        if (advance().current !is Token.Parens) {
            err("Expected (")
        }

        advance().parseCallArgs().also { advance() }
    } else {
        null
    }

    val body = parseScope()

    ItemKind.Constructor(modifiers, params, primaryCall, body)
}