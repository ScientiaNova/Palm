package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.Trait
import com.scientianova.palm.parser.data.types.TraitStatement
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqType
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

private fun parseTraitBody(parser: Parser) = recBuildList<TraitStatement> {
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
                    Token.Val -> TraitStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> TraitStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> TraitStatement.Method(parseFun(parser.advance(), modifiers))
                    Token.Type -> parseAssociatedType(parser.advance())
                    else -> parser.err(unexpectedMember("trait"))
                }
            )
        }
    }
}

private fun parseAssociatedType(parser: Parser): TraitStatement {
    val name = parseIdent(parser)
    val bound = parseTypeAnn(parser)
    val default = parseEqType(parser)

    return TraitStatement.AssociatedType(name, bound, default)
}

fun parseTrait(parser: Parser, modifiers: List<DecModifier>): Trait {
    val name = parseIdent(parser)
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    val superTraits = if (parser.current == Token.Colon) parseTypes(parser.advance()) else emptyList()
    parseWhere(parser, constraints)
    val body = if (parser.current == Token.LBrace) {
        parseTraitBody(parser.advance())
    } else {
        emptyList()
    }

    return Trait(name, modifiers, typeParams, constraints, superTraits, body)
}