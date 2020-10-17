package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.errors.*
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.identTokens
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.*
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.top.parseAnnotation
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at

fun parseType(parser: Parser): PType? {
    val normalType: PType = when (parser.current) {
        in identTokens -> parseNamedType(parser)
        Token.LParen -> parseTypeTuple(parser)
        Token.At -> parser.mark().end(Type.Annotated(parseAnnotation(parser), requireType(parser)))
        else -> return null
    }
    return parseTypeNullability(parser, normalType)
}

fun requireType(parser: Parser) = parseType(parser) ?: parser.err(missingType)

fun parseTypeAnn(parser: Parser) = if (parser.current == Token.Colon) {
    parseTypeBinOps(parser.advance())
} else {
    null
}

fun parseEqType(parser: Parser) = if (parser.current == Token.Assign) {
    requireTypeBinOps(parser.advance())
} else {
    null
}

fun requireEqType(parser: Parser) = if (parser.current == Token.Assign) {
    requireTypeBinOps(parser.advance())
} else {
    parser.err(missingType)
}

fun requireTypeAnn(parser: Parser) = parseTypeAnn(parser) ?: parser.err(missingTypeAnn)

private fun parseTypePath(parser: Parser): List<PString> = recBuildList {
    add(parseIdent(parser))

    if (parser.current == Token.Dot) {
        parser.advance()
    } else {
        return this
    }
}

fun parseTypeArgs(parser: Parser): List<PTypeArg> = recBuildList {
    if (parser.current == Token.RBracket) {
        return this
    } else {
        add(parseTypeArg(parser))
        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RBracket -> return this
            else -> parser.err(unclosedSquareBracket)
        }
    }
}

private fun parseNamedType(parser: Parser): PType {
    val start = parser.mark()

    val path = parseTypePath(parser)

    val args = if (parser.current == Token.LBracket && !parser.lastNewline) {
        parser.advance()

        val list = parser.withFlags(trackNewline = false, excludeCurly = false) {
            parseTypeArgs(parser)
        }

        parser.advance()

        list
    } else {
        emptyList()
    }

    return start.end(Type.Named(path, args))
}

private tailrec fun parseTypeNullability(parser: Parser, type: PType): PType =
    if (parser.current == Token.QuestionMark) {
        parseTypeNullability(
            parser.advance(),
            if (type.value !is Type.Nullable) {
                Type.Nullable(type).at(type.start, parser.pos)
            } else {
                type
            }
        )
    } else {
        type
    }

private fun parseInterTypeBody(parser: Parser, first: PType) = recBuildList(mutableListOf(first)) {
    add(requireType(parser))
    if (parser.current == Token.Plus) {
        parser.advance()
    } else return this
}

fun parseTypeBinOps(parser: Parser): PType? {
    val start = parser.mark()

    val first = parseType(parser) ?: return null

    return if (parser.current == Token.Plus) {
        start.end(Type.Intersection(parseInterTypeBody(parser.advance(), first)))
    } else {
        first
    }
}

fun requireTypeBinOps(parser: Parser) = parseTypeBinOps(parser) ?: parser.err(missingType)

private fun parseTypeTupleBody(parser: Parser): List<FunTypeArg> = recBuildList {
    if (parser.current == Token.RParen) {
        return this
    } else {
        val using = parser.current == Token.Using
        if (using) parser.advance()

        add(FunTypeArg(requireTypeBinOps(parser), using))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

private fun parseTypeTuple(parser: Parser): PType {
    val marker = parser.mark()

    parser.advance()

    val list = parser.withFlags(trackNewline = false, excludeCurly = false) {
        parseTypeTupleBody(parser)
    }

    parser.advance()

    return when {
        parser.current == Token.Arrow -> marker.end(Type.Function(list, requireType(parser)))
        list.size == 1 -> {
            val type = list[0]
            if (type.using) parser.err(missingTypeReturnType)
            type.type
        }
        else -> parser.err(missingTypeReturnType)
    }
}

private fun parseTypeArg(parser: Parser): PTypeArg = when (parser.current) {
    Token.Wildcard -> parser.advance().end(TypeArg.Wildcard)
    Token.In -> parseNormalTypeArg(parser, VarianceMod.In)
    Token.Out -> parseNormalTypeArg(parser, VarianceMod.Out)
    else -> {
        val type = requireTypeBinOps(parser)
        TypeArg.Normal(type, VarianceMod.None).at(type.start, type.next)
    }
}

private fun parseNormalTypeArg(parser: Parser, variance: VarianceMod): PTypeArg {
    val start = parser.mark()

    val type = requireType(parser.advance())

    return start.end(TypeArg.Normal(type, variance))
}