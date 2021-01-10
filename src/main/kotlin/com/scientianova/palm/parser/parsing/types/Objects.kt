package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.PDecMod
import com.scientianova.palm.parser.data.types.ObjStmt
import com.scientianova.palm.parser.data.types.Object
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseItem
import com.scientianova.palm.util.recBuildList

fun Parser.parseObjectBody() = recBuildList<ObjStmt> {
    when (current) {
        Token.End -> return this
        Token.Semicolon -> advance()
        initIdent -> add(ObjStmt.Initializer(advance().requireScope()))
        else -> {
            val modifiers = parseDecModifiers()
            when (current) {
                initIdent -> add(ObjStmt.Initializer(advance().requireScope()))
                else -> parseItem(modifiers)?.let { add(ObjStmt.Item(it)) }
            }
        }
    }
}

fun Parser.parseObject(modifiers: List<PDecMod>): Object {
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty(Parser::parseObjectBody)

    return Object(name, modifiers, superTypes, body)
}