package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseStatements

fun Parser.parseTypeClass(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val typeParams = parseTypeParams()
    val superTypes = parseInterfaceSuperTypes()
    val constraints = parseWhere()
    val body = inBracesOrEmpty { parseStatements() }

    return Statement.TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}