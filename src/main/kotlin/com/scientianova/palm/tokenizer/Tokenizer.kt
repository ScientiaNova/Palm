package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.GREEK_QUESTION_MARK_ERROR
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import java.util.*

typealias TokenList = LinkedList<PToken>

fun tokenize(code: String, fileName: String = "REPL"): TokenList {
    val state = ParseState(code, fileName)
    val queue = LinkedList<PToken>()
    return tokenize(state, state.pop(), queue)
}

tailrec fun tokenize(state: ParseState, char: Char?, list: TokenList): TokenList = when {
    char == null -> list
    char.isWhitespace() -> tokenize(state, state.pop(), list)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(
            state, char, char.isUpperCase(), list, state.lastPos, StringBuilder()
        )
        list.offer(identifier)
        tokenize(state, next, list)
    }
    char == '#' -> when (state.char()) {
        '[' -> tokenize(state, handleMultiLineComment(state, state.pop()), list)
        else -> tokenize(state, handleSingleLineComment(state, state.pop()), list)
    }
    else -> {
        val (token, next) = handleToken(state, char, list)
        list.offer(token)
        tokenize(state, next, list)
    }
}

fun handleToken(state: ParseState, char: Char, list: TokenList): Pair<PToken, Char?> = when (char) {
    '0' -> {
        val startPos = state.lastPos
        when (state.char()) {
            'x', 'X' -> {
                state.pop()
                handleHexNumber(state, state.pop(), startPos, StringBuilder())
            }
            'b', 'B' -> {
                state.pop()
                handleBinaryNumber(state, state.pop(), startPos, StringBuilder())
            }
            else -> handleNumber(state, char, state.lastPos, StringBuilder())
        }
    }
    in '1'..'9' -> handleNumber(state, char, state.lastPos, StringBuilder())
    '.' ->
        if (state.char()?.isDigit() == true) handleNumber(state, char, state.lastPos, StringBuilder())
        else handleMisc(state, char)
    '"' -> {
        val startPos = state.lastPos
        val next = state.pop()
        if (next == '"' && state.char() == '"') {
            state.pop()
            handleMultiLineString(state, state.pop(), startPos, list, emptyList(), StringBuilder())
        } else handleSingleLineString(state, next, startPos, list, emptyList(), StringBuilder())
    }
    '\'' -> handleChar(state, state.pop())
    '(' -> OpenParenToken at state.lastPos to state.pop()
    ')' -> ClosedParenToken at state.lastPos to state.pop()
    '[' -> (if (state.char() == '|') OpenArrayBracketToken else OpenSquareBracketToken) at state.lastPos to state.pop()
    ']' -> ClosedSquareBracketToken at state.lastPos to state.pop()
    '{' -> OpenCurlyBracketToken at state.lastPos to state.pop()
    '}' -> ClosedCurlyBracketToken at state.lastPos to state.pop()
    ',' -> CommaToken at state.lastPos to state.pop()
    ';' -> SemicolonToken at state.lastPos to state.pop()
    ';' -> state.error(GREEK_QUESTION_MARK_ERROR, state.lastPos)
    '|' ->
        if (state.char() == ']') ClosedArrayBracketToken at state.lastPos to state.pop()
        else handleMisc(state, char)
    else -> handleMisc(state, char)
}

fun handleMisc(
    state: ParseState,
    char: Char
): Pair<PToken, Char?> {
    val previous = state.beforePopped
    val symbolRes = handleSymbol(state, char, state.lastPos, StringBuilder())
    val symbolString = symbolRes.first.value
    val next = symbolRes.second
    SYMBOL_MAP[symbolString]?.let { return it at symbolRes.first.area to next }
    return (
            if ((previous == null || previous.isWhitespace() || previous.isSeparator() || previous.isOpenBracket()))
                if (next?.isWhitespace() == false) PrefixOperatorToken(symbolString)
                else InfixOperatorToken(symbolString)
            else
                if (next?.isWhitespace() == false && !next.isClosedBracket()) InfixOperatorToken(symbolString)
                else PostfixOperatorToken(symbolString)
            ) at symbolRes.first.area to next
}

tailrec fun handleSymbol(
    state: ParseState,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder
): Pair<Positioned<String>, Char?> =
    if (char == null || char.isLetterOrDigit() || char.isWhitespace() || char == '"' || char == '\'' || char.isBracket()) {
        val area = startPos until state.lastPos
        builder.toString() at area to char
    } else handleSymbol(state, state.pop(), startPos, builder.append(char))