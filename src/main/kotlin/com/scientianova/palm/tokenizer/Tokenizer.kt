package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.GREEK_QUESTION_MARK_ERROR
import com.scientianova.palm.errors.UNKNOWN_SYMBOL_ERROR
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
        else handleSymbol(traverser, char)
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
    '[' -> OpenSquareBracketToken on traverser.lastPos to traverser.pop()
    ']' -> ClosedSquareBracketToken on traverser.lastPos to traverser.pop()
    '{' -> OpenCurlyBracketToken on traverser.lastPos to traverser.pop()
    '}' -> ClosedCurlyBracketToken on traverser.lastPos to traverser.pop()
    ';' -> traverser.error(GREEK_QUESTION_MARK_ERROR, traverser.lastPos)
    else -> handleMisc(traverser, char, list)
}

fun handleMisc(
    traverser: StringTraverser,
    char: Char,
    list: TokenList
): Pair<PositionedToken, Char?> {
    val symbolRes = handleSymbol(traverser, char)
    val second = symbolRes.second
    return if (symbolRes.first.value is NotToken && second != null && second.isLetter() && second.isLowerCase()) {
        val identifierRes = handleIdentifier(traverser, char)
        when (identifierRes.first.value) {
            is IsToken -> IsNotToken on symbolRes.first.area.start..identifierRes.first.area.end to identifierRes.second
            is InToken -> NotInToken on symbolRes.first.area.start..identifierRes.first.area.end to identifierRes.second
            else -> {
                list.offer(symbolRes.first)
                identifierRes
            }
        }
    } else symbolRes
}


fun handleSymbol(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos = traverser.lastPos,
    builder: StringBuilder = StringBuilder()
): Pair<PositionedToken, Char?> =
    if (char == null || char.isLetterOrDigit() || char.isWhitespace() || char == '"' || char == '\'' || char.isBracket()) {
        val area = startPos..traverser.lastPos.shift(-1)
        (symbolMap[builder.toString()] ?: traverser.error(UNKNOWN_SYMBOL_ERROR, area)) on area to char
    } else handleSymbol(traverser, traverser.pop(), startPos, builder.append(char))

private val brackets = listOf('(', ')', '[', ']', '{', '}')
fun Char.isBracket() = this in brackets