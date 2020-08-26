package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.StringPos

private val char: Parser<Any, CharExpr> = matchChar<Any>('\'', missingCharError).takeR { state, succ, cErr, _ ->
    when (val char = state.char) {
        null, '\n' -> cErr(loneSingleQuoteError, state.area)
        '\\' -> when (val res = handleEscaped(state.next)) {
            is ParseResult.Success -> finishChar(res.next, res.value, succ, cErr)
            is ParseResult.Error -> cErr(res.error, res.area)
        }
        else -> finishChar(state.next, char, succ, cErr)
    }
}

@Suppress("UNCHECKED_CAST")
fun <R> char() = char as Parser<R, CharExpr>

private fun <R> finishChar(
    endState: ParseState,
    value: Char,
    succFn: SuccFn<R, CharExpr>,
    errFn: ErrFn<R>
): R {
    val endChar = endState.char
    return when {
        endChar == '\'' -> succFn(CharExpr(value), endState.next)
        endChar == null -> errFn(unclosedCharLiteralError, endState.area)
        endChar == ' ' && value == ' ' -> isMalformedTab(endState.next)?.let {
            errFn(malformedTabError, it.pos - 2..it.pos)
        } ?: errFn(missingSingleQuoteError, endState.area)
        else -> errFn(if (value == '\'') missingSingleQuoteOnQuoteError else missingSingleQuoteError, endState.area)
    }
}


fun handleEscaped(state: ParseState) = when (state.char) {
    '"' -> '\"' succTo state.next
    '$' -> '$' succTo state.next
    '\\' -> '\\' succTo state.next
    't' -> '\t' succTo state.next
    'n' -> '\n' succTo state.next
    'b' -> '\b' succTo state.next
    'r' -> '\r' succTo state.next
    'f' -> 12.toChar() succTo state.next
    'v' -> 11.toChar() succTo state.next
    'u' -> if (state.nextChar == '{') {
        handleUnicode(state.code, state.pos + 2)
    } else missingBacketInUnicodeError errAt state.nextPos
    else -> unclosedEscapeCharacterError errAt state.pos
}

private tailrec fun handleUnicode(
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

private fun isMalformedTab(state: ParseState): ParseState? = when (state.char) {
    null -> null
    ' ' -> isMalformedTab(state.next)
    '\'' -> state
    else -> null
}

fun Char.isIdentifierStart() = isLetter() || this == '_'
fun Char.isIdentifierPart() = isLetterOrDigit() || this == '_'

fun Char.isSymbolPart() = !(isWhitespace() || isIdentifierPart() || isBracket() || isSeparator() || isQuote())

fun Char?.isAfterPostfix() = this == null || isWhitespace() || isClosedBracket() || isSeparator()