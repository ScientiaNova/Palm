package com.scientianova.palm.lexer

import com.scientianova.palm.errors.unclosedMultilineComment
import com.scientianova.palm.util.StringPos

tailrec fun lexSingleLineComment(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> Token.EOF to pos + 1
    '\n' -> Token.EOL to pos + 1
    else -> lexSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun lexMultiLineComment(code: String, start: StringPos, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> unclosedMultilineComment.token(start, pos)
    '*' -> if (code.getOrNull(pos + 1) == '/') {
        Token.Comment to pos + 2
    } else {
        lexMultiLineComment(code, start, pos + 1)
    }
    '/' -> if (code.getOrNull(pos + 1) == '*') {
        val token = lexMultiLineComment(code, pos, pos + 2)
        if (token.first is Token.Error) token
        else lexMultiLineComment(code, start, token.second)
    } else {
        lexMultiLineComment(code, start, pos + 1)
    }
    else -> lexMultiLineComment(code, start, pos + 1)
}

tailrec fun lexWhitespace(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(code, pos + 1)
    else -> Token.Whitespace to pos
}