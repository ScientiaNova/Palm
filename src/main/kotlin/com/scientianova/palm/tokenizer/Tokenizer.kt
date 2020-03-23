package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.GREEK_QUESTION_MARK_ERROR
import com.scientianova.palm.errors.UNKNOWN_BINARY_OPERATOR_ERROR
import com.scientianova.palm.errors.UNKNOWN_UNARY_OPERATOR_ERROR
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

fun tokenize(traverser: StringTraverser, char: Char?, list: TokenList): TokenList = when {
    char == null -> list
    char.isWhitespace() -> tokenize(traverser, traverser.pop(), list)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(traverser, char)
        list.offer(identifier)
        tokenize(traverser, next, list)
    }
    char == '#' -> tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        tokenize(traverser, next, list)
    } else {
        list.offer(OpenCurlyBracketToken on traverser.lastPos)
        tokenize(traverser, traverser.pop(), list)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    } else {
        val (token, next) = handleMisc(traverser, char, list)
        list.offer(token)
        tokenize(traverser, next, list)
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
                handleBinaryNumber(traverser, traverser.pop(), list, startPos)
            }
            else -> handleNumber(traverser, char, list)
        }
    }
    in '1'..'9' -> handleNumber(traverser, char, list)
    '.' ->
        if (traverser.peek()?.isDigit() == true) handleNumber(traverser, char, list)
        else handleMisc(traverser, char, list)
    '"' -> {
        val startPos = traverser.lastPos
        val next = traverser.pop()
        if (next == '"' && traverser.peek() == '"') {
            traverser.pop()
            handleMultiLineString(traverser, traverser.pop(), startPos)
        } else handleSingleLineString(traverser, next, startPos)
    }
    '\'' -> handleChar(traverser, traverser.pop())
    '(' -> OpenParenToken on traverser.lastPos to traverser.pop()
    ')' -> {
        val bracketPos = traverser.lastPos
        val next = traverser.pop()
        if (next == '(') {
            list.offer(ClosedParenToken on bracketPos)
            TimesToken on traverser.lastPos to traverser.pop()
        } else ClosedParenToken on traverser.lastPos to next
    }
    '[' -> {
        val previous = traverser.beforePopped
        (if (previous != null && (previous.isLetterOrDigit() || previous == '"' || previous.isClosedBracket())) GetBracketToken
        else OpenSquareBracketToken) on traverser.lastPos to traverser.pop()
    }
    ']' -> ClosedSquareBracketToken on traverser.lastPos to traverser.pop()
    '}' -> ClosedCurlyBracketToken on traverser.lastPos to traverser.pop()
    ';' -> traverser.error(GREEK_QUESTION_MARK_ERROR, traverser.lastPos)
    else -> handleMisc(traverser, char, list)
}

fun handleMisc(
    traverser: StringTraverser,
    char: Char,
    list: TokenList
): Pair<PositionedToken, Char?> {
    val previous = traverser.beforePopped
    val symbolRes = handleSymbol(traverser, char)
    val symbolString = symbolRes.first.value
    val next = symbolRes.second
    if ((previous == null || previous.isWhitespace() || previous.isSeparator()) && next?.isWhitespace() == false)
        return (UNARY_OPS_MAP[symbolString] ?: SYMBOL_MAP[symbolString]
        ?: traverser.error(UNKNOWN_UNARY_OPERATOR_ERROR, symbolRes.first.area)) on symbolRes.first.area to next
    val symbol = BINARY_OPS_MAP[symbolString] ?: SYMBOL_MAP[symbolString]
    ?: traverser.error(UNKNOWN_BINARY_OPERATOR_ERROR, symbolRes.first.area)
    return if (symbol is NotToken && next != null && next.isLetter() && next.isLowerCase()) {
        val identifierRes = handleIdentifier(traverser, char)
        when (identifierRes.first.value) {
            is IsToken -> IsNotToken on symbolRes.first.area.start..identifierRes.first.area.end to identifierRes.second
            is InToken -> NotInToken on symbolRes.first.area.start..identifierRes.first.area.end to identifierRes.second
            else -> {
                list.offer(symbol on symbolRes.first.area)
                identifierRes
            }
        }
    } else symbol on symbolRes.first.area to next
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