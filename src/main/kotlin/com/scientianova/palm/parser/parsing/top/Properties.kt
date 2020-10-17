package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.errors.unclosedParenthesis
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.top.Getter
import com.scientianova.palm.parser.data.top.Property
import com.scientianova.palm.parser.data.top.Setter
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqExpr
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireBinOps

fun parseProperty(parser: Parser, modifiers: List<DecModifier>, mutable: Boolean): Property {
    val name = parseIdent(parser)
    val type = parseTypeAnn(parser)

    if (parser.current == Token.By) {
        val delegate = requireBinOps(parser.advance())
        return Property.Delegated(name, modifiers, mutable, type, delegate)
    }

    val expr = parseEqExpr(parser)

    val getterModifiers: List<DecModifier>
    val getter: Getter?
    val setterModifiers: List<DecModifier>
    val setter: Setter?

    val firstModsStart = parser.mark()
    val firstModifiers = parseDecModifiers(parser)

    when (parser.current) {
        Token.Get -> {
            getterModifiers = firstModifiers
            getter = parseGetter(parser)

            val secondModsStart = parser.mark()
            val secondModifiers = parseDecModifiers(parser)

            if (parser.current == Token.Set) {
                setterModifiers = secondModifiers
                setter = parseSetter(parser)
            } else {
                secondModsStart.revertIndex()
                setterModifiers = emptyList()
                setter = null
            }
        }
        Token.Set -> {
            setterModifiers = firstModifiers
            setter = parseSetter(parser)

            val secondModsStart = parser.mark()
            val secondModifiers = parseDecModifiers(parser)

            if (parser.current == Token.Get) {
                getterModifiers = secondModifiers
                getter = parseGetter(parser)
            } else {
                secondModsStart.revertIndex()
                getterModifiers = emptyList()
                getter = null
            }
        }
        else -> {
            firstModsStart.revertIndex()
            getterModifiers = emptyList()
            getter = null
            setterModifiers = emptyList()
            setter = null
        }
    }

    return Property.Normal(name, modifiers, mutable, type, expr, getterModifiers, getter, setterModifiers, setter)
}

private fun parseGetter(parser: Parser) = if (parser.current == Token.LParen) {
    if (parser.advance().current != Token.RParen) {
        parser.err(unclosedParenthesis)
    }

    parser.advance()

    val type = parseTypeAnn(parser)
    val expr = requireFunBody(parser)

    Getter(type, expr)
} else {
    null
}


private fun parseSetter(parser: Parser) = if (parser.current == Token.LParen) {
    val param = parseOptionallyTypedFunParam(parser.advance())

    if (parser.current != Token.RParen) {
        parser.err(unclosedParenthesis)
    }

    parser.advance()

    val type = parseTypeAnn(parser)
    val expr = requireFunBody(parser)

    Setter(param, type, expr)
} else {
    null
}