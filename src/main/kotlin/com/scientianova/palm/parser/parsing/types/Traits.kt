package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.TypeClass
import com.scientianova.palm.parser.data.types.TCStmt
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseEqType
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

private fun parseTraitBody(parser: Parser) = recBuildList<TCStmt> {
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
                    Token.Val -> TCStmt.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> TCStmt.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> TCStmt.Method(parseFun(parser.advance(), modifiers))
                    Token.Type -> parseAssociatedType(parser.advance())
                    else -> parser.err(unexpectedMember("trait"))
                }
            )
        }
    }
}

private fun parseAssociatedType(parser: Parser): TCStmt {
    val name = parseIdent(parser)
    val bound = parseTypeAnn(parser)
    val default = parseEqType(parser)

    return TCStmt.AssociatedType(name, bound, default)
}

fun parseTrait(parser: Parser, modifiers: List<DecModifier>): TypeClass {
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

    return TypeClass(name, modifiers, typeParams, constraints, superTraits, body)
}