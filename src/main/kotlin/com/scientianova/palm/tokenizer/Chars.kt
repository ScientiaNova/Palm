package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

data class CharToken(val char: Char) : IToken

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
    return when {
        end == '\'' -> CharToken(value) on startPos..traverser.lastPos to traverser.pop()
        char.isWhitespace() && value.isWhitespace() && isMalformedTab(traverser, traverser.pop()) ->
            traverser.error(MALFORMED_TAB_ERROR, startPos..traverser.lastPos)
        else -> traverser.error(
            if (value == '\'') MISSING_SINGLE_QUOTE_ON_QUOTE_ERROR else MISSING_SINGLE_QUOTE_ERROR,
            traverser.lastPos
        )
    }
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
    'u' ->
        if (traverser.pop() == '{') handleUnicode(traverser, traverser.pop())
        else traverser.error(MISSING_BRACKET_IN_UNICODE_ERROR, traverser.lastPos)
    else -> null
}

tailrec fun handleUnicode(
    traverser: StringTraverser,
    char: Char?,
    idBuilder: StringBuilder = StringBuilder()
): Pair<Char, Char?> = when (char) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' ->
        handleUnicode(traverser, traverser.pop(), idBuilder.append(char))
    '}'  -> idBuilder.toString().toInt().toChar() to char
    else -> traverser.error(INVALID_HEX_LITERAL_ERROR, traverser.lastPos)
}

fun Char.isOpenBracket() = when (this) {
    '(', '[', '{' -> true
    else -> false
}

fun Char.isClosedBracket() = when (this) {
    ')', ']', '}' -> true
    else -> false
}

fun Char.isBracket() = when (this) {
    '(', ')', '[', ']', '{', '}' -> true
    else -> false
}

fun Char.isSeparator() = when (this) {
    ',', ';' -> true
    else -> false
}

fun isMalformedTab(traverser: StringTraverser, char: Char?): Boolean = when (char) {
    null -> false
    ' ' -> isMalformedTab(traverser, traverser.pop())
    '\'' -> true
    else -> false
}