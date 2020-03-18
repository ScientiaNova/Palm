package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.on
import java.util.*

typealias TokenList = LinkedList<PositionedToken>

fun tokenize(code: String): TokenList {
    val traverser = StringTraverser(code)
    val queue = LinkedList<PositionedToken>()
    return tokenize(traverser, traverser.pop(), queue)
}

fun tokenize(traverser: StringTraverser, char: Char?, list: TokenList): TokenList = when {
    char == null -> list
    char.isWhitespace() -> tokenize(traverser, traverser.pop(), list)
    char.isJavaIdentifierStart() -> {
        val row = traverser.row
        val (identifier, next) = handleIdentifier(traverser, char)
        list.offer(identifier on row)
        tokenize(traverser, next, list)
    }
    char == '#' -> tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        tokenize(traverser, next, list)
    } else {
        list.offer(OpenCurlyBracketToken on traverser.row)
        tokenize(traverser, traverser.pop(), list)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        tokenize(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    } else {
        val row = traverser.row
        val (token, next) = handleMisc(traverser, char, list, row)
        list.offer(token on row)
        tokenize(traverser, next, list)
    }
    else -> {
        val row = traverser.row
        val (token, next) = handleToken(traverser, char, list)
        list.offer(token on row..traverser.lastRow)
        tokenize(traverser, next, list)
    }
}

fun handleToken(traverser: StringTraverser, char: Char, list: TokenList): Pair<IToken, Char?> = when (char) {
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
    in '1'..'9' -> {
        val row = traverser.row
        val numRes = handleNumber(traverser, char)
        val second = numRes.second
        if (second != null && (second.isLetter() || second == '(')) {
            when {
                second.isLetter() -> {
                    list.offer(numRes.first on row)
                    val identifier = handleIdentifier(traverser, second)
                    list.offer(TimesToken on row)
                    identifier
                }
                second == '(' -> {
                    list.offer(numRes.first on row)
                    list.offer(TimesToken on row)
                    OpenParenToken to traverser.pop()
                }
                else -> numRes
            }
        } else numRes
    }
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
    ')' -> {
        val next = traverser.pop()
        if (next == '(') {
            list.offer(ClosedParenToken on traverser.row)
            TimesToken to traverser.pop()
        } else ClosedParenToken to next
    }
    '[' -> OpenSquareBracketToken to traverser.pop()
    ']' -> ClosedSquareBracketToken to traverser.pop()
    '{' -> OpenCurlyBracketToken to traverser.pop()
    '}' -> ClosedCurlyBracketToken to traverser.pop()
    else -> handleMisc(traverser, char, list, traverser.row)
}

private fun handleMisc(traverser: StringTraverser, char: Char, list: TokenList, row: Int): Pair<IToken, Char?> {
    val symbolRes = handleSymbol(traverser, char)
    val second = symbolRes.second
    return if (symbolRes.first is NotToken && second != null && second.isLetter() && second.isLowerCase()) {
        val identifierRes = handleIdentifier(traverser, char)
        when (identifierRes.first) {
            is IsToken -> IsNotToken to identifierRes.second
            is InToken -> NotInToken to identifierRes.second
            else -> {
                list.offer(symbolRes.first on row)
                identifierRes
            }
        }
    } else symbolRes
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