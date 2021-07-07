package com.palmlang.palm.parser.types

import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.parser.expressions.requireType
import com.palmlang.palm.parser.top.parseContextParams
import com.palmlang.palm.parser.top.parseScope
import com.palmlang.palm.parser.top.parseStatements

fun Parser.parseImpl(): Statement {
    val typeParams = parseTypeParams()
    val type = requireType()
    val context = parseContextParams()
    val constraints = parseWhere()
    val statements = parseScope()

    return Statement.Implementation(type, typeParams, constraints, context, statements)
}