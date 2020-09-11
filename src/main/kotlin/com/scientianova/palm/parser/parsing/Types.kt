package com.scientianova.palm.parser.parsing

import com.scientianova.palm.errors.*
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.identTokens
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.types.*
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.at
import com.scientianova.palm.util.end

fun parseType(parser: Parser): PType = parseTypeNullability(
    parser, when (parser.current) {
        in identTokens -> parseNamedType(parser)
        Token.LParen -> parseTypeTuple(parser)
        else -> parser.err(missingType)
    }
)

fun parseTypeAnnotation(parser: Parser) = if (parser.current == Token.Colon) {
    parseType(parser.advance())
} else {
    null
}

fun parseTypePath(parser: Parser): List<PString> = recBuildList<PString> {
    val ident = parser.current.identString()

    if (ident.isEmpty()) parser.err(missingIdentifier)
    parser.advance()

    add(parser.end(ident))

    if (parser.current == Token.Dot) {
        parser.advance()
    } else return this
}

fun parseTypeArgs(parser: Parser): List<PTypeArg> = recBuildList<PTypeArg> {
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

fun parseNamedType(parser: Parser): PType {
    val start = parser.Marker()

    val path = parseTypePath(parser)

    val args = if (parser.current == Token.LBracket && !parser.lastNewline) {
        parser.advance()
        parser.trackNewline = false

        val list = parseTypeArgs(parser)

        parser.advance()
        start.revertFlags()

        list
    } else {
        emptyList()
    }

    return start.end(Type.Named(path, args))
}

tailrec fun parseTypeNullability(parser: Parser, type: PType): PType = if (parser.current == Token.QuestionMark) {
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

fun parseInterTypeBody(parser: Parser, first: PType): List<PType> = recBuildList(mutableListOf(first)) {
    add(parseType(parser))
    if (parser.current == Token.Plus) {
        parser.advance()
    } else return this
}

fun parseTypeBinOps(parser: Parser): PType {
    val start = parser.Marker()

    val first = parseType(parser)

    return if (parser.current == Token.Plus) {
        start.end(Type.Intersection(parseInterTypeBody(parser.advance(), first)))
    } else {
        first
    }
}

private fun parseTypeTupleBody(parser: Parser): List<FunTypeArg> = recBuildList<FunTypeArg> {
    if (parser.current == Token.RParen) {
        return this
    } else {
        val using = parser.current == Token.Using
        if (using) parser.advance()

        add(FunTypeArg(parseTypeBinOps(parser), using))

        when (parser.current) {
            Token.Comma -> parser.advance()
            Token.RParen -> return this
            else -> parser.err(unclosedParenthesis)
        }
    }
}

fun parseTypeTuple(parser: Parser): PType {
    val marker = parser.Marker()

    parser.advance()
    parser.trackNewline = false

    val list = parseTypeTupleBody(parser)

    parser.advance()
    marker.revertFlags()

    return when {
        parser.current == Token.Arrow -> marker.end(Type.Function(list, parseType(parser)))
        list.size == 1 -> {
            val type = list[0]
            if (type.using) parser.err(missingTypeReturnType)
            type.type
        }
        else -> parser.err(missingTypeReturnType)
    }
}

fun parseTypeArg(parser: Parser): PTypeArg = when (parser.current) {
    Token.Wildcard -> parser.advance().end(TypeArg.Wildcard)
    Token.In -> parseNormalTypeArg(parser, VarianceMod.In)
    Token.Out -> parseNormalTypeArg(parser, VarianceMod.Out)
    else -> {
        val type = parseTypeBinOps(parser)
        TypeArg.Normal(type, VarianceMod.None).at(type.start, type.end)
    }
}

fun parseNormalTypeArg(parser: Parser, variance: VarianceMod): PTypeArg {
    val start = parser.Marker()

    val type = parseType(parser.advance())

    return start.end(TypeArg.Normal(type, variance))
}