package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.types.ImplStatement
import com.scientianova.palm.parser.data.types.Implementation
import com.scientianova.palm.parser.data.types.TraitImplStatement
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireEqType
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

private fun parseImplBody(parser: Parser) = recBuildList<ImplStatement> {
    when (parser.current) {
        Token.RBrace -> {
            parser.advance()
            return this
        }
        Token.Semicolon -> parser.advance()
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> ImplStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> ImplStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> ImplStatement.Method(parseFun(parser.advance(), modifiers))
                    else -> parser.err(unexpectedMember("implementation"))
                }
            )
        }
    }
}

private fun parseTraitImplBody(parser: Parser) = recBuildList<TraitImplStatement> {
    when (parser.current) {
        Token.RBrace -> {
            parser.advance()
            return this
        }
        Token.Semicolon -> parser.advance()
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> TraitImplStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> TraitImplStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> TraitImplStatement.Method(parseFun(parser.advance(), modifiers))
                    Token.Type -> parseAssociatedType(parser)
                    else -> parser.err(unexpectedMember("implementation"))
                }
            )
        }
    }
}

private fun parseAssociatedType(parser: Parser): TraitImplStatement =
    TraitImplStatement.AssociatedType(parseIdent(parser), requireEqType(parser))

fun parseImpl(parser: Parser): Implementation {
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    val type = requireType(parser)

    return if (parser.current == Token.For) {
        val forType = requireType(parser.advance())
        parseWhere(parser, constraints)
        val body = if (parser.current == Token.LBrace) {
            parseTraitImplBody(parser.advance())
        } else {
            emptyList()
        }

        Implementation.Trait(type, forType, typeParams, constraints, body)
    } else {
        parseWhere(parser, constraints)
        val body = parseImplBody(parser.advance())

        Implementation.Inherent(type, typeParams, constraints, body)
    }
}