package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

tailrec fun lexSingleLineComment(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null, '\n' -> pos
    else -> lexSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun lexMultiLineComment(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null -> pos
    '*' ->  {
        if (code.getOrNull(pos + 1) == '/') pos + 2
        else lexMultiLineComment(code, pos + 1)
    }
    '/' ->  {
        if (code.getOrNull(pos + 1) == '*') {
            val newPos = lexMultiLineComment(code, pos + 2)
            lexMultiLineComment(code, newPos)
        } else lexMultiLineComment(code, pos + 1)
    }
    else -> lexMultiLineComment(code, pos + 1)
}