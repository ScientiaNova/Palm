package com.scientianova.palm.parser.parsing.types

import com.scientianova.palm.errors.unclosedSquareBracket
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.PType
import com.scientianova.palm.parser.parseIdent
import com.scientianova.palm.parser.parsing.expressions.parseTypeAnn
import com.scientianova.palm.parser.recBuildList
import com.scientianova.palm.util.PString

fun parseTypeParams(parser: Parser, constraints: MutableList<Pair<PString, PType>>): List<PString> =
    if (parser.current == Token.LBracket) {
        recBuildList {
            if (parser.current == Token.RBracket) {
                parser.advance()
                return this
            } else {
                val param = parseIdent(parser)
                parseTypeAnn(parser)?.let { constraints.add(param to it) }
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