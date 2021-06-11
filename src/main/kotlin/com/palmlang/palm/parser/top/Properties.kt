package com.palmlang.palm.parser.top

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.lexer.getIdent
import com.palmlang.palm.lexer.setIdent
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.Getter
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.ast.top.Setter
import com.palmlang.palm.parser.expressions.parseEqExpr
import com.palmlang.palm.parser.expressions.parseTypeAnn
import com.palmlang.palm.parser.expressions.requireDecPattern
import com.palmlang.palm.parser.expressions.requireEqExpr

fun Parser.parseProperty(modifiers: List<PDecMod>): Statement {
    val pattern = requireDecPattern()
    val context = parseContextParams()
    val type = parseTypeAnn()

    val expr = parseEqExpr()

    val getterModifiers: List<PDecMod>
    val getter: Getter?
    val setterModifiers: List<PDecMod>
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

    return Statement.Property(
        pattern,
        modifiers,
        type,
        context,
        expr,
        getterModifiers,
        getter,
        setterModifiers,
        setter
    )
}

private fun Parser.parseGetter(): Getter? {
    val parens = current
    return if (parens is Token.Parens) {
        if (parens.tokens.size != 1) {
            err("Getter needs to have no parameters")
        }

        val type = advance().parseTypeAnn()
        val expr = requireEqExpr()

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
        val expr = requireEqExpr()

        Setter(param, type, expr)
    } else {
        null
    }
}