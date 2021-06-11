package com.palmlang.palm.parser.types

import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.top.parseStatements

fun Parser.parseObject(modifiers: List<PDecMod>): Statement {
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty { parseStatements() }

    return Statement.Object(name, modifiers, superTypes, body)
}