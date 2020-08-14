package com.scientianova.palm.parser

import com.scientianova.palm.util.StringPos

data class ParseState(val code: String, val pos: StringPos) {
    val lastPos get() = pos - 1
    val nextPos get() = pos + 1

    val lastChar get() = code.getOrNull(pos - 1)
    val char get() = code.getOrNull(pos)
    val nextChar get() = code.getOrNull(pos + 1)

    val last get() = copy(pos = pos - 1)
    val next get() = copy(pos = pos + 1)

    val actual get() = actual(this)
    val actualOrBreak get() = actualOrBreak(this)

    val nextActual get() = next.actual
    val nextActualOrBreak get() = next.actualOrBreak

    fun startWith(string: String) = code.startsWith(string, startIndex = pos)

    operator fun plus(places: Int) = copy(pos = pos + places)
}

tailrec fun actual(state: ParseState): ParseState {
    val char = state.char
    return when {
        char == null -> state
        char.isWhitespace() -> actual(state)
        char == '/' -> when (state.nextChar) {
            '/' -> actual(handleSingleLineComment(state + 2))
            '*' -> actual(handleMultiLineComment(state + 2))
            else -> state
        }
        else -> state
    }
}

tailrec fun actualOrBreak(state: ParseState): ParseState {
    val char = state.char
    return when {
        char == null -> state
        char == '\n' -> state
        char.isWhitespace() -> actualOrBreak(state)
        char == '/' -> when (state.nextChar) {
            '/' -> actualOrBreak(handleSingleLineComment(state + 2))
            '*' -> actualOrBreak(handleMultiLineComment(state + 2))
            else -> state
        }
        else -> state
    }
}