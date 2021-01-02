package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

tailrec fun Lexer.lexSingleLineComment(code: String, pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    null -> Token.Whitespace.add(pos)
    '\n' -> Token.EOL.add(pos + 1)
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
        val nested = Lexer(pos, errors = errors).lexMultiLineComment(code, pos + 2)
        lexMultiLineComment(code, nested.pos)
    } else {
        lexMultiLineComment(code, pos + 1)
    }
    else -> lexMultiLineComment(code, pos + 1)
}

tailrec fun Lexer.lexWhitespace(code: String, pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(code, pos + 1)
    else -> Token.Whitespace.add(pos)
}