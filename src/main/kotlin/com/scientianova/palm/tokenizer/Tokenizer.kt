package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.GREEK_QUESTION_MARK_ERROR
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import java.util.*

typealias TokenList = LinkedList<PositionedToken>

fun tokenize(code: String, fileName: String = "REPL"): TokenList {
    val traverser = StringTraverser(code, fileName)
    val queue = LinkedList<PositionedToken>()
    return tokenize(traverser, traverser.pop(), queue)
}

tailrec fun tokenize(traverser: StringTraverser, char: Char?, list: TokenList): TokenList = when {
    char == null -> list
    char.isWhitespace() -> tokenize(traverser, traverser.pop(), list)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(traverser, char, list)
        list.offer(identifier)
        tokenize(traverser, next, list)
    }
    char == '#' -> when (traverser.peek()) {
        '[' -> tokenize(traverser, handleMultiLineComment(traverser, traverser.pop()), list)
        else -> tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    }
    else -> {
        val (token, next) = handleToken(traverser, char, list)
        list.offer(token)
        tokenize(traverser, next, list)
    }
}

fun handleToken(traverser: StringTraverser, char: Char, list: TokenList): Pair<PositionedToken, Char?> = when (char) {
    '0' -> {
        val startPos = traverser.lastPos
        when (traverser.peek()) {
            'x', 'X' -> {
                traverser.pop()
                handleHexNumber(traverser, traverser.pop(), startPos)
            }
            'b', 'B' -> {
                traverser.pop()
                handleBinaryNumber(traverser, traverser.pop(), startPos)
            }
            else -> handleNumber(traverser, char)
        }
    }
    in '1'..'9' -> handleNumber(traverser, char)
    '`' -> {
        val startPos = traverser.lastPos
        handleBacktickedIdentifier(traverser, traverser.pop(), startPos)
    }
    '.' ->
        if (traverser.peek()?.isDigit() == true) handleNumber(traverser, char)
        else handleMisc(traverser, char)
    '"' -> {
        val startPos = traverser.lastPos
        val next = traverser.pop()
        if (next == '"' && traverser.peek() == '"') {
            traverser.pop()
            handleMultiLineString(traverser, traverser.pop(), startPos, list)
        } else handleSingleLineString(traverser, next, startPos, list)
    }
    '\'' -> handleChar(traverser, traverser.pop())
    '(' -> OpenParenToken on traverser.lastPos to traverser.pop()
    ')' -> ClosedParenToken on traverser.lastPos to traverser.pop()
    '[' -> (if (traverser.peek() == '|') OpenArrayBracketToken else OpenSquareBracketToken) on traverser.lastPos to traverser.pop()
    ']' -> ClosedSquareBracketToken on traverser.lastPos to traverser.pop()
    '{' -> OpenCurlyBracketToken on traverser.lastPos to traverser.pop()
    '}' -> ClosedCurlyBracketToken on traverser.lastPos to traverser.pop()
    ';' -> traverser.error(GREEK_QUESTION_MARK_ERROR, traverser.lastPos)
    '|' ->
        if (traverser.peek() == ']') ClosedArrayBracketToken on traverser.lastPos to traverser.pop()
        else handleMisc(traverser, char)
    else -> handleMisc(traverser, char)
}

fun handleMisc(
    traverser: StringTraverser,
    char: Char
): Pair<PositionedToken, Char?> {
    val previous = traverser.beforePopped
    val symbolRes = handleSymbol(traverser, char)
    val symbolString = symbolRes.first.value
    val next = symbolRes.second
    SYMBOL_MAP[symbolString]?.let { return it on symbolRes.first.area to next }
    return (
            if ((previous == null || previous.isWhitespace() || previous.isSeparator() || previous.isOpenBracket()))
                if (next?.isWhitespace() == false)
                    PREFIX_OPS_MAP[symbolString] ?: PrefixOperatorToken(symbolString)
                else BINARY_OPS_MAP[symbolString] ?: InfixOperatorToken(symbolString)
            else
                if (next?.isWhitespace() == false && !next.isClosedBracket())
                    BINARY_OPS_MAP[symbolString] ?: InfixOperatorToken(symbolString)
                else POSTFIX_OPS_MAP[symbolString] ?: InfixOperatorToken(symbolString)
            ) on symbolRes.first.area to next
}

fun handleSymbol(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<String>, Char?> =
    if (char == null || char.isLetterOrDigit() || char.isWhitespace() || char == '"' || char == '\'' || char.isBracket()) {
        val area = startPos..traverser.lastPos.shift(-1)
        builder.toString() on area to char
    } else handleSymbol(traverser, traverser.pop(), startPos, builder.append(char))