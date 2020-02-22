package com.scientianovateam.palm.tokenizer

import com.scientianovateam.palm.on
import java.util.*

typealias TokenStack = Stack<PositionedToken>

fun tokenize(code: String): TokenStack {
    val traverser = StringTraverser(code)
    val stack = TokenStack()
    return tokenize(traverser, traverser.pop(), stack)
}

fun tokenize(traverser: StringTraverser, char: Char?, stack: TokenStack): TokenStack = when {
    char == null -> stack
    char.isWhitespace() -> tokenize(traverser, traverser.pop(), stack)
    char.isJavaIdentifierStart() -> if (char.isUpperCase()) {
        val row = traverser.row
        val (identifier, next) = handleCapitalizedIdentifier(traverser, char)
        stack.push(identifier on row)
        tokenize(traverser, next, stack)
    } else {
        val row = traverser.row
        val (identifier, next) = handleUncapitalizedIdentifier(traverser, char)
        stack.push(identifier on row)
        tokenize(traverser, next, stack)
    }
    char == '#' -> tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), stack)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        tokenize(traverser, next, stack)
    } else {
        stack.push(OpenCurlyBracketToken on traverser.row)
        tokenize(traverser, traverser.pop(), stack)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), stack)
    } else {
        val row = traverser.row
        val (symbol, next) = handleSymbol(traverser, char)
        stack.push(symbol on row)
        tokenize(traverser, next, stack)
    }
    else -> {
        val row = traverser.row
        val (token, next) = handleToken(traverser, char)
        stack.push(token on row..traverser.lastRow)
        tokenize(traverser, next, stack)
    }
}

fun handleToken(traverser: StringTraverser, char: Char): Pair<IToken, Char?> = when (char) {
    '0' -> when (traverser.peek()) {
        'x', 'X' -> {
            traverser.pop()
            handleHexNumber(traverser, traverser.pop())
        }
        'b', 'B' -> {
            traverser.pop()
            handleBinaryNumber(traverser, traverser.pop())
        }
        else -> handleNumber(traverser, char)
    }
    in '1'..'9' -> handleNumber(traverser, char)
    '.' ->
        if (traverser.peek()?.isDigit() == true) handleNumber(traverser, char)
        else handleSymbol(traverser, char)
    '"' -> {
        val next = traverser.pop()
        if (next == '"' && traverser.peek() == '"') {
            traverser.pop()
            handleMultiLineString(traverser, traverser.pop())
        } else handleSingleLineString(traverser, next)
    }
    '\'' -> handleChar(traverser, traverser.pop())
    '(' -> OpenParenToken to traverser.pop()
    ')' -> ClosedParenToken to traverser.pop()
    '[' -> OpenSquareBracketToken to traverser.pop()
    ']' -> ClosedSquareBracketToken to traverser.pop()
    '{' -> OpenCurlyBracketToken to traverser.pop()
    '}' -> ClosedCurlyBracketToken to traverser.pop()
    else -> handleSymbol(traverser, char)
}

fun handleSymbol(
    traverser: StringTraverser,
    char: Char?,
    builder: StringBuilder = StringBuilder()
): Pair<IToken, Char?> =
    if (char == null || char.isLetterOrDigit() || char.isWhitespace() || char == '"' || char.isBracket())
        handleSymbol(builder.toString()) to char
    else handleSymbol(traverser, traverser.pop(), builder.append(char))

private val brackets = listOf('(', ')', '[', ']', '{', '}')
fun Char.isBracket() = this in brackets