package com.scientianova.palm.lexer

import com.scientianova.palm.errors.unclosedMultilineComment
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

tailrec fun lexSingleLineComment(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> Token.EOF.at(pos)
    '\n' -> Token.EOL.at(pos)
    else -> lexSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun lexMultiLineComment(code: String, start: StringPos, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> unclosedMultilineComment.token(start, pos)
    '*' -> if (code.getOrNull(pos + 1) == '/') {
        Token.Comment.at(start, pos + 2)
    } else {
        lexMultiLineComment(code, start, pos + 1)
    }
    '/' -> if (code.getOrNull(pos + 1) == '*') {
        val token = lexMultiLineComment(code, pos, pos + 2)
        if (token.value is Token.Error) token
        else lexMultiLineComment(code, start, token.next)
    } else {
        lexMultiLineComment(code, start, pos + 1)
    }
    else -> lexMultiLineComment(code, start, pos + 1)
}

tailrec fun lexWhitespace(code: String, start: StringPos, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(code, start, pos + 1)
    else -> Token.Whitespace.at(start, pos)
}