package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.missingWildcard
import com.scientianova.palm.errors.unclosedSquareBracket
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.data.types.KindParam
import com.scientianova.palm.parser.data.types.PKindParam
import com.scientianova.palm.parser.data.types.TypeParam
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString

fun parseTypeParam(parser: Parser): TypeParam {
    val name = parseIdent(parser)
    val params = if (parser.current == Token.LBracket) {
        parseKinds(parser)
    } else {
        emptyList()
    }

    return TypeParam(name, params)
}

fun parseKind(parser: Parser): PKindParam {
    val start = parser.Marker()

    if (parser.current != Token.Wildcard) parser.err(missingWildcard)

    val params = if (parser.advance().current == Token.LBracket) {
        parseKinds(parser)
    } else {
        emptyList()
    }

    return start.end(KindParam(params))
}

fun parseKinds(parser: Parser): List<PKindParam> {
    parser.advance()

    recBuildList<PKindParam> {
        if (parser.current == Token.RBracket) {
            parser.advance()
            return this
        } else {
            add(parseKind(parser))
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
}

fun parseTypeParams(parser: Parser, constraints: MutableList<Pair<PString, PType>>): List<TypeParam> = if (parser.current == Token.LBracket) {
    recBuildList<TypeParam> {
        if (parser.current == Token.RBracket) {
            parser.advance()
            return this
        } else {
            val param = parseTypeParam(parser)
            parseTypeAnn(parser)?.let { constraints.add(param.name to it) }
            add(param)

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