package com.palmlang.palm.parser.types

import com.palmlang.palm.lexer.Token
import com.palmlang.palm.parser.Parser
import com.palmlang.palm.ast.expressions.PType
import com.palmlang.palm.ast.expressions.Statement
import com.palmlang.palm.ast.top.PDecMod
import com.palmlang.palm.parser.parseIdent
import com.palmlang.palm.parser.top.parseStatements
import com.palmlang.palm.parser.expressions.requireType
import com.palmlang.palm.parser.top.parseScope
import com.palmlang.palm.util.recBuildList

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
    val body = parseScope(name)

    return Statement.Interface(name, modifiers, typeParams, constraints, superTypes, body)
}