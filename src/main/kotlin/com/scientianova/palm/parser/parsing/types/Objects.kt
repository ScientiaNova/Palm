package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Statement
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseStatements

fun Parser.parseObject(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty { parseStatements() }

    return Statement.Object(name, modifiers, superTypes, body)
}