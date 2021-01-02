package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.initIdent
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.ObjStmt
import com.scientianova.palm.parser.data.types.TypeDec
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.parsing.top.parseTypeDec
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
                Token.Val -> add(ObjStmt.Property(advance().parseProperty(modifiers, false)))
                Token.Var -> add(ObjStmt.Property(advance().parseProperty(modifiers, true)))
                Token.Fun -> add(ObjStmt.Method(advance().parseFun(modifiers)))
                else -> parseTypeDec(modifiers)?.let { add(ObjStmt.NestedDec(it)) }
            }
        }
    }
}

fun Parser.parseObject(modifiers: List<DecModifier>): TypeDec {
    val name = parseIdent()
    val superTypes = parseClassSuperTypes()
    val body = inBracesOrEmpty(Parser::parseObjectBody)

    return TypeDec.Object(name, modifiers, superTypes, body)
}