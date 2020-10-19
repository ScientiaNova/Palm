package com.scientianova.palm.parser.parsing.expressions

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.expressions.Pattern

fun parsePattern(parser: Parser): Pattern = when (parser.current) {
    Token.Is -> Pattern.Type(requireTypeBinOps(parser.advance()))
    else -> Pattern.Expr(requireBinOps(parser))
}