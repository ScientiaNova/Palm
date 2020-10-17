package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.Mixin
import com.scientianova.palm.parser.data.types.MixinStatement
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireType
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

fun parseMixin(parser: Parser, modifiers: List<DecModifier>): Mixin {
    val name = parseIdent(parser)
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    val on = parseMixinTypes(parser)
    val body: List<MixinStatement>

    if (parser.current == Token.LBrace) {
        body = parseMixinBody(parser)
        parser.advance()
    } else {
        body = emptyList()
    }

    return Mixin(name, modifiers, typeParams, constraints, on, body)
}

private fun parseMixinTypes(parser: Parser): List<PType> = if (parser.current == Token.On) {
    recBuildList {
        add(requireType(parser))
        if (parser.current == Token.Comma) {
            parser.advance()
        } else {
            return this
        }
    }
} else {
    emptyList()
}

private fun parseMixinBody(parser: Parser) = recBuildList<MixinStatement> {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> MixinStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> MixinStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> MixinStatement.Method(parseFun(parser.advance(), modifiers))
                    else -> parser.err(unexpectedMember("mixin"))
                }
            )
        }
    }
}