package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.GREEK_QUESTION_MARK_ERROR
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import java.util.*

typealias TokenList = LinkedList<PToken>

fun tokenize(code: String, fileName: String = "REPL"): TokenList {
    val traverser = StringTraverser(code, fileName)
    val queue = LinkedList<PToken>()
    return tokenize(traverser, traverser.pop(), queue)
}

tailrec fun tokenize(traverser: StringTraverser, char: Char?, list: TokenList): TokenList = when {
    char == null -> list
    char.isWhitespace() -> tokenize(traverser, traverser.pop(), list)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(
            traverser, char, char.isUpperCase(), list, traverser.lastPos, StringBuilder()
        )
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

fun handleToken(traverser: StringTraverser, char: Char, list: TokenList): Pair<PToken, Char?> = when (char) {
    '0' -> {
        val startPos = traverser.lastPos
        when (traverser.peek()) {
            'x', 'X' -> {
                traverser.pop()
                handleHexNumber(traverser, traverser.pop(), startPos, StringBuilder())
            }
            'b', 'B' -> {
                traverser.pop()
                handleBinaryNumber(traverser, traverser.pop(), startPos, StringBuilder())
            }
            else -> handleNumber(traverser, char, traverser.lastPos, StringBuilder())
        }
    }
    in '1'..'9' -> handleNumber(traverser, char, traverser.lastPos, StringBuilder())
    '.' ->
        if (traverser.peek()?.isDigit() == true) handleNumber(traverser, char, traverser.lastPos, StringBuilder())
        else handleMisc(traverser, char)
    '"' -> {
        val startPos = traverser.lastPos
        val next = traverser.pop()
        if (next == '"' && traverser.peek() == '"') {
            traverser.pop()
            handleMultiLineString(traverser, traverser.pop(), startPos, list, emptyList(), StringBuilder())
        } else handleSingleLineString(traverser, next, startPos, list, emptyList(), StringBuilder())
    }
    '\'' -> handleChar(traverser, traverser.pop())
    '(' -> OpenParenToken on traverser.lastPos to traverser.pop()
    ')' -> ClosedParenToken on traverser.lastPos to traverser.pop()
    '[' -> (if (traverser.peek() == '|') OpenArrayBracketToken else OpenSquareBracketToken) on traverser.lastPos to traverser.pop()
    ']' -> ClosedSquareBracketToken on traverser.lastPos to traverser.pop()
    '{' -> OpenCurlyBracketToken on traverser.lastPos to traverser.pop()
    '}' -> ClosedCurlyBracketToken on traverser.lastPos to traverser.pop()
    ',' -> CommaToken on traverser.lastPos to traverser.pop()
    ';' -> SemicolonToken on traverser.lastPos to traverser.pop()
    ';' -> traverser.error(GREEK_QUESTION_MARK_ERROR, traverser.lastPos)
    '|' ->
        if (traverser.peek() == ']') ClosedArrayBracketToken on traverser.lastPos to traverser.pop()
        else handleMisc(traverser, char)
    else -> handleMisc(traverser, char)
}

fun handleMisc(
    traverser: StringTraverser,
    char: Char
): Pair<PToken, Char?> {
    val previous = traverser.beforePopped
    val symbolRes = handleSymbol(traverser, char, traverser.lastPos, StringBuilder())
    val symbolString = symbolRes.first.value
    val next = symbolRes.second
    SYMBOL_MAP[symbolString]?.let { return it on symbolRes.first.area to next }
    return (
            if ((previous == null || previous.isWhitespace() || previous.isSeparator() || previous.isOpenBracket()))
                if (next?.isWhitespace() == false) PrefixOperatorToken(symbolString)
                else InfixOperatorToken(symbolString)
            else
                if (next?.isWhitespace() == false && !next.isClosedBracket()) InfixOperatorToken(symbolString)
                else PostfixOperatorToken(symbolString)
            ) on symbolRes.first.area to next
}

tailrec fun handleSymbol(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    builder: StringBuilder
): Pair<Positioned<String>, Char?> =
    if (char == null || char.isLetterOrDigit() || char.isWhitespace() || char == '"' || char == '\'' || char.isBracket()) {
        val area = startPos..traverser.lastPos.shift(-1)
        builder.toString() on area to char
    } else handleSymbol(traverser, traverser.pop(), startPos, builder.append(char))