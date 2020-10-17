package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.missingScope
import com.scientianova.palm.errors.unexpectedMember
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.types.Extension
import com.scientianova.palm.parser.data.types.ExtensionStatement
import com.scientianova.palm.parser.data.types.ExtensionType
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.requireTypeBinOps
import com.scientianova.palm.parser.parsing.top.parseDecModifiers
import com.scientianova.palm.parser.parsing.top.parseFun
import com.scientianova.palm.parser.parsing.top.parseProperty
import com.scientianova.palm.parser.recBuildList

private fun parseExtendedType(parser: Parser): ExtensionType {
    val type = requireTypeBinOps(parser)
    val name = if (parser.current == Token.As) {
        parseIdent(parser)
    } else {
        null
    }

    return ExtensionType(type, name)
}

private fun parseExtensionTypes(parser: Parser) = recBuildList<ExtensionType> {
    add(parseExtendedType(parser))
    if (parser.current == Token.Comma) {
        parser.advance()
    } else {
        return this
    }
}

fun parseExtension(parser: Parser): Extension {
    val constraints = constraints()
    val typeParams = parseTypeParams(parser, constraints)
    val on = parseExtensionTypes(parser)
    parseWhere(parser, constraints)

    if (parser.current != Token.LBrace) {
        parser.err(missingScope)
    }

    val body = parseExtensionBody(parser.advance())
    parser.advance()

    return Extension(on, typeParams, constraints, body)
}

private fun parseExtensionBody(parser: Parser) = recBuildList<ExtensionStatement> {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        Token.Extend -> add(ExtensionStatement.Extension(parseExtension(parser.advance())))
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> ExtensionStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> ExtensionStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> ExtensionStatement.Method(parseFun(parser.advance(), modifiers))
                    else -> parser.err(unexpectedMember("extension"))
                }
            )
        }
    }
}