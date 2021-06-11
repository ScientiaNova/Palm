package com.palmlang.palm.parser.types

import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.top.parseStatements

fun Parser.parseTypeClass(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val typeParams = parseTypeParams()
    val superTypes = parseInterfaceSuperTypes()
    val constraints = parseWhere()
    val body = inBracesOrEmpty { parseStatements() }

    return Statement.TypeClass(name, modifiers, typeParams, constraints, superTypes, body)
}