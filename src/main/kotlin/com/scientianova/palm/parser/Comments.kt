package com.scientianova.palm.parser

tailrec fun handleSingleLineComment(state: ParseState): ParseState = when (state.char) {
    null, '\n' -> state
    else -> handleSingleLineComment(state.next)
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleMultiLineComment(state: ParseState): ParseState = when (state.char) {
    null -> state
    ']' ->  {
        if (state.nextChar == '#') state + 2
        else handleMultiLineComment(state.next)
    }
    '#' ->  {
        if (state.nextChar == '[') {
            val newNext = handleMultiLineComment(state + 2)
            handleMultiLineComment(newNext)
        } else handleMultiLineComment(state.next)
    }
    else -> handleMultiLineComment(state.next)
}