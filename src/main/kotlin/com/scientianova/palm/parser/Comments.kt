@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.util.StringPos

tailrec fun handleSingleLineComment(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null, '\n' -> pos
    else -> handleSingleLineComment(code, pos + 1)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleMultiLineComment(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null -> pos
    '*' ->  {
        if (code.getOrNull(pos + 1) == '/') pos + 2
        else handleMultiLineComment(code, pos + 1)
    }
    '/' ->  {
        if (code.getOrNull(pos + 1) == '*') {
            val newPos = handleMultiLineComment(code, pos + 2)
            handleMultiLineComment(code, newPos)
        } else handleMultiLineComment(code, pos + 1)
    }
    else -> handleMultiLineComment(code, pos + 1)
}

private val whitespace: Parser<Any, Unit> = { state, succ, _, _ -> succ(Unit, state.actual) }

fun <R> whitespace() = whitespace as Parser<R, Unit>

private val whitespaceOnLine: Parser<Any, Unit> = { state, succ, _, _ -> succ(Unit, state.actualOrBreak) }

fun <R> whitespaceOnLine() = whitespaceOnLine as Parser<R, Unit>