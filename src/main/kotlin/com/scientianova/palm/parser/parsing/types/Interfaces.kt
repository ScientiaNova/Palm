package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseStatements
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.util.recBuildList

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

fun Parser.parseInterface(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val typeParams = parseTypeParams()
    val superTypes = parseInterfaceSuperTypes()
    val constraints = parseWhere()
    val body = inBracesOrEmpty { parseStatements() }

    return Statement.Interface(name, modifiers, typeParams, constraints, superTypes, body)
}