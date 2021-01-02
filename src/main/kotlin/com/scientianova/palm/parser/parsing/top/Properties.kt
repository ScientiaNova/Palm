package com.scientianova.palm.parser.parsing.top

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.byIdent
import com.scientianova.palm.lexer.getIdent
import com.scientianova.palm.lexer.setIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqExpr
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.expressions.requireBinOps

fun Parser.parseProperty(modifiers: List<DecModifier>, mutable: Boolean): Property {
    val name = parseIdent()
    val context = parseContextParams()
    val type = parseTypeAnn()

    if (current === byIdent) {
        val delegate = advance().requireBinOps()
        return Property(name, modifiers, mutable, type, context, PropertyBody.Delegate(delegate))
    }

    val expr = parseEqExpr()

    val getterModifiers: List<DecModifier>
    val getter: Getter?
    val setterModifiers: List<DecModifier>
    val setter: Setter?

    val firstModsStart = index
    val firstModifiers = parseDecModifiers()

    when (current) {
        getIdent -> {
            advance()
            getterModifiers = firstModifiers
            getter = parseGetter()

            val secondModsStart = index
            val secondModifiers = parseDecModifiers()

            if (current === setIdent) {
                advance()
                setterModifiers = secondModifiers
                setter = parseSetter()
            } else {
                index = secondModsStart
                setterModifiers = emptyList()
                setter = null
            }
        }
        setIdent -> {
            advance()
            setterModifiers = firstModifiers
            setter = parseSetter()

            val secondModsStart = index
            val secondModifiers = parseDecModifiers()

            if (current === getIdent) {
                advance()
                getterModifiers = secondModifiers
                getter = parseGetter()
            } else {
                index = secondModsStart
                getterModifiers = emptyList()
                getter = null
            }
        }
        else -> {
            index = firstModsStart
            getterModifiers = emptyList()
            getter = null
            setterModifiers = emptyList()
            setter = null
        }
    }

    return Property(
        name,
        modifiers,
        mutable,
        type,
        context,
        PropertyBody.Normal(expr, getterModifiers, getter, setterModifiers, setter)
    )
}

private fun Parser.parseGetter(): Getter? {
    val parens = current
    return if (parens is Token.Parens) {
        if (parens.tokens.size != 1) {
            err("Getter needs to have no parameters")
        }

        val type = advance().parseTypeAnn()
        val expr = requireFunBody()

        Getter(type, expr)
    } else {
        null
    }
}


private fun Parser.parseSetter(): Setter? {
    val parens = current
    return if (parens is Token.Parens) {
        val param = parenthesizedOf(parens.tokens).parseOptionallyTypedFunParam()

        advance()

        val type = parseTypeAnn()
        val expr = requireFunBody()

        Setter(param, type, expr)
    } else {
        null
    }
}