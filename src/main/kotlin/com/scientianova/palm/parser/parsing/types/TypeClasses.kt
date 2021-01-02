package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.typeIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.TCStmt
import com.scientianova.palm.parser.data.types.TypeClass
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqType
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.parsing.top.parseTypeDec
import com.scientianova.palm.util.recBuildList

private fun Parser.parseTCBody() = recBuildList<TCStmt> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                Token.Val -> add(TCStmt.Property(advance().parseProperty(modifiers, false)))
                Token.Var -> add(TCStmt.Property(advance().parseProperty(modifiers, true)))
                Token.Fun -> add(TCStmt.Method(advance().parseFun(modifiers)))
                typeIdent -> add(parseAssociatedType())
                else -> parseTypeDec(modifiers)?.let { add(TCStmt.NestedDec(it)) }
            }
        }
    }
}

private fun Parser.parseAssociatedType(): TCStmt {
    val name = parseIdent()
    val bound = parseTypeAnn()
    val default = parseEqType()

    return TCStmt.AssociatedType(name, bound, default)
}

fun Parser.parseTypeClass(modifiers: List<DecModifier>): TypeClass {
    val name = parseIdent()
    val constraints = constraints()
    val typeParams = parseTypeParams(constraints)
    val superTypes = parseInterfaceSuperTypes()
    parseWhere(constraints)
    val body = current.let { brace ->
        if (brace is Token.Braces) {
            scopedOf(brace.tokens).parseTCBody()
        } else {
            emptyList()
        }
    }

    return TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}