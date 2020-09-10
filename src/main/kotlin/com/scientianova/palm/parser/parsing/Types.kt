package com.scientianova.palm.parser.parsing

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.data.types.PType

fun parseType(parser: Parser): PType = TODO()

fun parseTypeAnnotation(parser: Parser) = if (parser.current == Token.Colon) {
    parseType(parser.advance())
} else {
    null
}