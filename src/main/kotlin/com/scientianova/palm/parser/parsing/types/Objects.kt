package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.Object
import com.scientianova.palm.parser.data.types.ObjectStatement
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireScope
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

fun parseObjectBody(parser: Parser) = recBuildList<ObjectStatement> {
    when (parser.current) {
        Token.RBrace -> {
            parser.advance()
            return this
        }
        Token.Semicolon -> parser.advance()
        Token.Init -> add(ObjectStatement.Initializer(requireScope(parser.advance())))
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> ObjectStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> ObjectStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> ObjectStatement.Method(parseFun(parser.advance(), modifiers))
                    else -> parser.err(unexpectedMember("object"))
                }
            )
        }
    }
}

fun parseObject(parser: Parser, modifiers: List<DecModifier>): Object {
    val name = parseIdent(parser)
    val superTypes = parseSuperTypes(parser)
    val body = if (parser.current == Token.LBrace) {
        parseObjectBody(parser.advance())
    } else {
        emptyList()
    }

    return Object(name, modifiers, superTypes, body)
}