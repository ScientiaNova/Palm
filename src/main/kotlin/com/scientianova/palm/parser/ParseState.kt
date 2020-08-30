package com.scientianova.palm.parser

import com.scientianova.palm.util.StringPos

data class ParseState(val code: String, val pos: StringPos) {
    val area get() = pos..pos

    val lastPos get() = pos - 1
    val nextPos get() = pos + 1

    val char get() = code.getOrNull(pos)
    val nextChar get() = code.getOrNull(pos + 1)

    val last get() = copy(pos = pos - 1)
    val next get() = copy(pos = pos + 1)

    val actual get() = when (char) {
        '\n', '\t', '\r', ' ' -> ParseState(code, actual(code, nextPos))
        else -> this
    }

    val actualOrBreak get() = when (char) {
        '\t', '\r', ' ' -> ParseState(code, actualOrBreak(code, nextPos))
        else -> this
    }

    val nextActual get() = next.actual
    val nextActualOrBreak get() = next.actualOrBreak

    fun startsWith(string: String) = code.startsWith(string, startIndex = pos)
    fun startsWithIdent(string: String) = startsWith(string)
            && code.getOrNull(pos + string.length)?.isIdentifierPart() != true

    fun startsWithSymbol(string: String) = startsWith(string)
            && code.getOrNull(pos + string.length)?.isSymbolPart() != true

    operator fun plus(places: Int) = copy(pos = pos + places)
}

tailrec fun actual(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null -> pos
    '\n', ' ', '\t', '\r' -> actual(code, pos + 1)
    '/' -> when (code.getOrNull(pos + 1)) {
        '/' -> actual(code, handleSingleLineComment(code, pos + 2))
        '*' -> actual(code, handleMultiLineComment(code, pos + 2))
        else -> pos
    }
    else -> pos
}

tailrec fun actualOrBreak(code: String, pos: StringPos): StringPos = when (code.getOrNull(pos)) {
    null -> pos
    '\n' -> pos
    ' ', '\t', '\r' -> actualOrBreak(code, pos + 1)
    '/' -> when (code.getOrNull(pos + 1)) {
        '/' -> actualOrBreak(code, handleSingleLineComment(code, pos + 2))
        '*' -> actualOrBreak(code, handleMultiLineComment(code, pos + 2))
        else -> pos
    }
    else -> pos
}