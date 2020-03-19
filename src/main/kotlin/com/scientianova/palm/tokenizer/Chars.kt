package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_ESCAPE_CHARACTER_ERROR
import com.scientianova.palm.errors.LONE_SINGLE_QUOTE_ERROR
import com.scientianova.palm.errors.MISSING_SINGLE_QUOTE_ERROR
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

fun handleChar(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos
): Pair<Positioned<CharToken>, Char?> {
    val (value, end) = when (char) {
        null, '\n' -> traverser.error(LONE_SINGLE_QUOTE_ERROR, traverser.lastPos)
        '\\' -> handleEscaped(traverser, traverser.pop())
            ?: traverser.error(INVALID_ESCAPE_CHARACTER_ERROR, traverser.lastPos)
        else -> char to traverser.pop()
    }
    return if (end == '\'') CharToken(value) on startPos..traverser.lastPos to traverser.pop()
    else traverser.error(MISSING_SINGLE_QUOTE_ERROR, traverser.lastPos)
}

fun handleEscaped(traverser: StringTraverser, char: Char?) = when (char) {
    '"' -> '\"' to traverser.pop()
    '$' -> '$' to traverser.pop()
    '\\' -> '\\' to traverser.pop()
    't' -> '\t' to traverser.pop()
    'n' -> '\n' to traverser.pop()
    'b' -> '\b' to traverser.pop()
    'r' -> '\r' to traverser.pop()
    'f' -> 12.toChar() to traverser.pop()
    'v' -> 11.toChar() to traverser.pop()
    'u' -> handleUnicode(traverser, traverser.pop())
    else -> null
}

fun handleUnicode(
    traverser: StringTraverser,
    char: Char?,
    idBuilder: StringBuilder = StringBuilder()
): Pair<Char, Char?> = when (char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' -> {
        idBuilder.append(char)
        if (idBuilder.length == 4) idBuilder.toString().toInt(radix = 16).toChar() to traverser.pop()
        else handleUnicode(traverser, traverser.pop(), idBuilder)
    }
    else -> idBuilder.toString().toInt().toChar() to char
}