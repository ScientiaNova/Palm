package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun handleChar(
    state: ParseState
): ParseResult<Positioned<CharExpr>> = when (val char = state.char) {
    null, '\n' -> loneSingleQuoteError errAt state.pos
    '\\' -> handleEscaped(state.next)
    else -> char succTo state.next
}.flatMap { value, endState ->
    val endChar = endState.char
    when {
        endChar == '\'' -> CharExpr(value) at state.lastPos..endState.pos succTo endState.next
        endChar == null -> unclosedCharLiteralError errAt endState.pos
        endChar.isWhitespace() && value.isWhitespace() -> isMalformedTab(endState.next)?.let {
            malformedTabError errAt state.lastPos..it.pos
        } ?: missingSingleQuoteError errAt endState.pos
        else -> (if (value == '\'') missingSingleQuoteOnQuoteError else missingSingleQuoteError)
            .errAt(endState.pos)
    }
}


fun handleEscaped(state: ParseState) = when (state.char) {
    '"' -> '\"' succTo state + 1
    '$' -> '$' succTo state + 1
    '\\' -> '\\' succTo state + 1
    't' -> '\t' succTo state + 1
    'n' -> '\n' succTo state + 1
    'b' -> '\b' succTo state + 1
    'r' -> '\r' succTo state + 1
    'f' -> 12.toChar() succTo state + 1
    'v' -> 11.toChar() succTo state + 1
    'u' -> if (state.nextChar == '{') {
        handleUnicode(state.code, state.pos + 2)
    } else missingBacketInUnicodeError errAt state.nextPos
    else -> unclosedEscapeCharacterError errAt state.pos
}

tailrec fun handleUnicode(
    code: String,
    pos: StringPos,
    idBuilder: StringBuilder = StringBuilder()
): ParseResult<Char> = when (val char = code.getOrNull(pos)) {
    in '0'..'9', in 'a'..'f', in 'A'..'F' ->
        handleUnicode(code, pos + 1, idBuilder.append(char))
    '}' -> idBuilder.toString().toInt().toChar() succTo ParseState(code, pos + 1)
    else -> invalidHexLiteralError errAt pos
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

fun Char.isIdentifierStart() = isLetter() || this == '_'
fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'

fun Char.isSymbolPart() = !(isWhitespace() || isIdentifierPart() || isBracket() || isSeparator() || isQuote())

fun Char?.isAfterPostfix() = this == null || isWhitespace() || isClosedBracket() || isSeparator()