package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

tailrec fun lexSingleLineComment(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> Token.EOF to pos + 1
    '\n' -> Token.EOL to pos + 1
    else -> lexSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun lexMultiLineComment(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> Token.Error("Unclosed multi-line comment") to pos
    '*' -> if (code.getOrNull(pos + 1) == '/') {
        Token.Comment to pos + 2
    } else {
        lexMultiLineComment(code, pos + 1)
    }
    '/' -> if (code.getOrNull(pos + 1) == '*') {
        val token = lexMultiLineComment(code, pos + 2)
        lexMultiLineComment(code, token.second)
    } else {
        lexMultiLineComment(code, pos + 1)
    }
    else -> lexMultiLineComment(code, pos + 1)
}

tailrec fun lexWhitespace(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(code, pos + 1)
    else -> Token.Whitespace to pos
}