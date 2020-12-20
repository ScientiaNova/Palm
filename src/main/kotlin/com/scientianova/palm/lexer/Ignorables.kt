package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

tailrec fun lexSingleLineComment(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    null -> Token.Whitespace.till(pos)
    '\n' -> Token.EOL.till(pos + 1)
    else -> lexSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun Lexer.lexMultiLineComment(code: String, pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    null -> addErr("Unclosed multi-line comment", this.pos + 1, pos)
    '*' -> if (code.getOrNull(pos + 1) == '/') {
        Token.Comment.add(pos + 2)
    } else {
        lexMultiLineComment(code, pos + 1)
    }
    '/' -> if (code.getOrNull(pos + 1) == '*') {
        val nested = Lexer().lexMultiLineComment(code, pos + 2)
        syncErrors(nested).lexMultiLineComment(code, nested.pos)
    } else {
        lexMultiLineComment(code, pos + 1)
    }
    else -> lexMultiLineComment(code, pos + 1)
}

tailrec fun lexWhitespace(code: String, pos: StringPos): PToken = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(code, pos + 1)
    else -> Token.Whitespace.till(pos)
}