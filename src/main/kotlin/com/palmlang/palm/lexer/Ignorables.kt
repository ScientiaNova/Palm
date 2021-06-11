package com.palmlang.palm.lexer

import com.palmlang.palm.util.StringPos

tailrec fun Lexer.lexSingleLineComment(pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    null -> Token.Whitespace.add(pos)
    '\n' -> Token.EOL.add(pos + 1)
    else -> lexSingleLineComment(pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun Lexer.lexMultiLineComment(pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    null -> addErr("Unclosed multi-line comment", this.pos + 1, pos)
    '*' -> if (code.getOrNull(pos + 1) == '/') {
        Token.Comment.add(pos + 2)
    } else {
        lexMultiLineComment(pos + 1)
    }
    '/' -> if (code.getOrNull(pos + 1) == '*') {
        val nested = Lexer(code, filePath, pos, errors = errors).lexMultiLineComment(pos + 2)
        lexMultiLineComment(nested.pos)
    } else {
        lexMultiLineComment(pos + 1)
    }
    else -> lexMultiLineComment(pos + 1)
}

tailrec fun Lexer.lexWhitespace(pos: StringPos): Lexer = when (code.getOrNull(pos)) {
    ' ', '\t', '\r' -> lexWhitespace(pos + 1)
    else -> Token.Whitespace.add(pos)
}