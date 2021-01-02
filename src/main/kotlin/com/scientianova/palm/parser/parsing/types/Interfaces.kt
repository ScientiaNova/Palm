package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.InterfaceStmt
import com.scientianova.palm.parser.data.types.TypeDec
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.parsing.top.parseTypeDec
import com.scientianova.palm.util.recBuildList

fun Parser.parseInterfaceBody() = recBuildList<InterfaceStmt> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                Token.Val -> add(InterfaceStmt.Property(advance().parseProperty(modifiers, false)))
                Token.Var -> add(InterfaceStmt.Property(advance().parseProperty(modifiers, true)))
                Token.Fun -> add(InterfaceStmt.Method(advance().parseFun(modifiers)))
                else -> parseTypeDec(modifiers)?.let { add(InterfaceStmt.NestedDec(it)) }
            }
        }
    }
}

fun Parser.parseInterfaceSuperTypes(): List<PType> = if (current == Token.Colon) {
    recBuildList {
        add(requireType())
        if (current == Token.Comma) {
            advance()
        } else {
            return this
        }
    }
} else {
    emptyList()
}

fun Parser.parseInterface(modifiers: List<DecModifier>): TypeDec {
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = inParensOrEmpty { parseClassTypeParams(constraints) }
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = inBracesOrEmpty(Parser::parseInterfaceBody)

    return TypeDec.Interface(name, modifiers, typeParams, constraints, superTypes, body)
}