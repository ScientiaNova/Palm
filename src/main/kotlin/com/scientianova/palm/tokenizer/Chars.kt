package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.*
import com.scientianova.palm.parser.CharExpr
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun handleChar(
    state: ParseState
): Pair<Positioned<CharExpr>, ParseState> {
    val (value, endState) = when (val char = state.char) {
        null, '\n' -> LONE_SINGLE_QUOTE_ERROR throwAt state.pos
        '\\' -> handleEscaped(state.next)
            ?: INVALID_ESCAPE_CHARACTER_ERROR throwAt state.nextPos
        else -> char to state.next
    }
    val endChar = endState.char
    return when {
        endChar == '\'' -> CharExpr(value) at state.lastPos..endState.pos to endState.next
        endChar == null -> UNCLOSED_CHAR_LITERAL_ERROR throwAt endState.pos
        endChar.isWhitespace() && value.isWhitespace() -> isMalformedTab(endState.next)?.let {
            MALFORMED_TAB_ERROR throwAt state.lastPos..it.pos
        } ?: MISSING_SINGLE_QUOTE_ERROR throwAt endState.pos
        else -> (if (value == '\'') MISSING_SINGLE_QUOTE_ON_QUOTE_ERROR else MISSING_SINGLE_QUOTE_ERROR)
            .throwAt(endState.pos)
    }
}

fun handleEscaped(state: ParseState) = when (state.char) {
    '"' -> '\"' to state + 1
    '$' -> '$' to state + 1
    '\\' -> '\\' to state + 1
    't' -> '\t' to state + 1
    'n' -> '\n' to state + 1
    'b' -> '\b' to state + 1
    'r' -> '\r' to state + 1
    'f' -> 12.toChar() to state + 1
    'v' -> 11.toChar() to state + 1
    'u' ->
        if (state.nextChar == '{') handleUnicode(state.code, state.pos + 2)
        else MISSING_BRACKET_IN_UNICODE_ERROR throwAt state.nextPos
    else -> null
}

tailrec fun handleUnicode(
    code: String,
    pos: StringPos,
    idBuilder: StringBuilder = StringBuilder()
): Pair<Char, ParseState> = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' ->
        handleUnicode(code, pos + 1, idBuilder.append(char))
    '}' -> idBuilder.toString().toInt().toChar() to ParseState(code, pos + 1)
    else -> INVALID_HEX_LITERAL_ERROR throwAt pos
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

fun Char.isQuote() = when (this) {
    '\"', '\'' -> true
    else -> false
}

fun isMalformedTab(state: ParseState): ParseState? = when (state.char) {
    null -> null
    ' ' -> isMalformedTab(state.next)
    '\'' -> state
    else -> null
}

fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'
fun Char.isSymbolPart() = !(isIdentifierPart() || isBracket() || isSeparator() || isQuote())

fun Char.isLineSpace() = when (this) {
    ' ', '\t' -> true
    else -> false
}