package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.*
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Arg
import com.scientianova.palm.parser.data.expressions.VarianceMod
import com.scientianova.palm.parser.data.top.DecModifier
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.*
import com.scientianova.palm.parser.parsing.top.*
import com.scientianova.palm.parser.recBuildList

private fun parseSuperType(parser: Parser): SuperType {
    val type = requireType(parser)
    return when (parser.current) {
        Token.By -> {
            val delegate = parseIdent(parser.advance())
            SuperType.Interface(type, delegate)
        }
        Token.LParen -> {
            val args = parseCallArgs(parser)
            val mixins = if (parser.current == Token.On) {
                if (parser.advance().current == Token.LParen) {
                    parser.advance()
                    recBuildList {
                        if (parser.current == Token.RParen) {
                            parser.advance()
                            return@recBuildList this
                        }

                        add(requireType(parser))

                        when (parser.current) {
                            Token.Comma -> parser.advance()
                            Token.RParen -> {
                                parser.advance()
                                return@recBuildList this
                            }
                            else -> {
                            }
                        }
                    }
                } else {
                    listOf(requireType(parser))
                }
            } else {
                emptyList()
            }
            SuperType.Class(type, args, mixins)
        }
        else -> SuperType.Interface(type, null)
    }
}

fun parseSuperTypes(parser: Parser): List<SuperType> = if (parser.current == Token.Colon) {
    recBuildList {
        add(parseSuperType(parser))
        if (parser.current == Token.Comma) {
            parser.advance()
        } else {
            return this
        }
    }
} else {
    emptyList()
}

private fun parsePrimaryParam(parser: Parser): PrimaryParam {
    val modifiers = parseParamModifiers(parser)
    val decHandling = when (parser.current) {
        Token.Val -> {
            parser.advance()
            DecHandling.Val
        }
        Token.Var -> {
            parser.advance()
            DecHandling.Var
        }
        else -> DecHandling.None
    }
    val name = parseIdent(parser)
    val type = requireTypeAnn(parser)
    val default = parseEqExpr(parser)
    return PrimaryParam(modifiers, decHandling, name, type, default)
}

private fun parsePrimaryParams(parser: Parser) = parser.withFlags(trackNewline = false, excludeCurly = false) {
    recBuildList<PrimaryParam> {
        if (parser.current == Token.RParen) {
            return@recBuildList this
        }

        add(parsePrimaryParam(parser))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return@recBuildList this
            else -> parser.err(unclosedParenthesis)
        }
    }
}


fun parseClass(parser: Parser, modifiers: List<DecModifier>): Class {
    val name = parseIdent(parser)

    val constraints = constraints()
    val typeParams = parseClassTypeParams(parser, constraints)

    val constructorModifiers = parseDecModifiers(parser)
    val atConstructor = parser.current == Token.Constructor

    if (!(constructorModifiers.isEmpty() || atConstructor)) {
        parser.err(missingConstructor)
    }

    if (atConstructor) {
        parser.advance()
    }

    val primaryConstructor: List<PrimaryParam>?

    if (parser.current == Token.LParen) {
        primaryConstructor = parsePrimaryParams(parser.advance())
        parser.advance()
    } else {
        primaryConstructor = null
    }

    val superTypes = parseSuperTypes(parser)

    parseWhere(parser, constraints)

    val body: List<ClassStatement>
    if (parser.current == Token.LBrace) {
        body = parseClassBody(parser)
        parser.advance()
    } else {
        body = emptyList()
    }

    return Class(name, modifiers, constructorModifiers, primaryConstructor, typeParams, constraints, superTypes, body)
}

private fun parseClassTypeParams(parser: Parser, constraints: Constraints): List<PClassTypeParam> =
    if (parser.current == Token.LBracket) {
        recBuildList {
            if (parser.current == Token.RBracket) {
                parser.advance()
                return this
            } else {
                val start = parser.mark()
                val variance = when (parser.current) {
                    Token.In -> {
                        parser.advance()
                        VarianceMod.In
                    }
                    Token.Out -> {
                        parser.advance()
                        VarianceMod.Out
                    }
                    else -> VarianceMod.None
                }
                val param = parseIdent(parser)
                parseTypeAnn(parser)?.let { constraints.add(param to it) }
                add(start.end(ClassTypeParam(param, variance)))

                when (parser.current) {
                    Token.Comma -> parser.advance()
                    Token.RBracket -> {
                        parser.advance()
                        return this
                    }
                    else -> parser.err(unclosedSquareBracket)
                }
            }
        }
    } else {
        emptyList()
    }

private fun parseClassBody(parser: Parser) = recBuildList<ClassStatement> {
    when (parser.current) {
        Token.RBrace -> return this
        Token.Semicolon -> parser.advance()
        Token.Init -> add(ClassStatement.Initializer(requireScope(parser.advance())))
        else -> {
            val modifiers = parseDecModifiers(parser)
            add(
                when (parser.current) {
                    Token.Val -> ClassStatement.Property(parseProperty(parser.advance(), modifiers, false))
                    Token.Var -> ClassStatement.Property(parseProperty(parser.advance(), modifiers, true))
                    Token.Fun -> ClassStatement.Method(parseFun(parser.advance(), modifiers))
                    Token.Constructor -> parseConstructor(parser, modifiers)
                    else -> parser.err(unexpectedMember("class"))
                }
            )
        }
    }
}

private fun parseConstructor(parser: Parser, modifiers: List<DecModifier>): ClassStatement {
    if (parser.current != Token.LParen) {
        parser.err(unexpectedSymbol("("))
    }

    val params = parseFunParams(parser.advance())
    parser.advance()

    val primaryCall: List<Arg>?

    if (parser.current == Token.Colon) {
        if (parser.advance().current != Token.This) {
            parser.err(missingThis)
        }

        if (parser.advance().current != Token.LParen) {
            parser.err(unexpectedSymbol("("))
        }

        primaryCall = parseCallArgs(parser)
        parser.advance()
    } else {
        primaryCall = null
    }

    val body = parseScope(parser) ?: emptyList()

    return ClassStatement.Constructor(modifiers, params, primaryCall, body)
}