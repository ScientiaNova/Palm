package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.on
import java.util.*

fun handleSingleLineString(
    traverser: StringTraverser,
    char: Char?,
    parts: List<StringTokenPart> = emptyList(),
    builder: StringBuilder = StringBuilder()
): Pair<StringToken, Char?> = when (char) {
    null, '\n' -> error("Missing Double quote")
    '"' ->
        (if (parts.isEmpty()) PureStringToken(builder.toString())
        else StringTemplateToken(if (builder.isEmpty()) parts else parts + StringPart(builder))) to traverser.pop()
    '$' -> traverser.pop().let { next ->
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val row = traverser.row
                val (identifier, newNext) = handleIdentifier(traverser, next)
                handleSingleLineString(traverser, newNext, parts + StringPart(builder) + TokensPart(identifier on row))
            }
            next == '{' -> {
                val stack = LinkedList<PositionedToken>()
                handleInterpolation(traverser, traverser.pop(), stack)
                handleSingleLineString(traverser, traverser.pop(), parts + StringPart(builder) + TokensPart(stack))
            }
            else -> handleSingleLineString(traverser, next, parts, builder.append(char))
        }
    }
    '\\' -> handleSingleLineString(
        traverser, traverser.pop(), parts,
        builder.append(handleEscaped(traverser, traverser.pop()))
    )
    else -> handleSingleLineString(traverser, traverser.pop(), parts, builder.append(char))
}

fun handleMultiLineString(
    traverser: StringTraverser,
    char: Char?,
    parts: List<StringTokenPart> = emptyList(),
    builder: StringBuilder = StringBuilder()
): Pair<StringTemplateToken, Char?> = when (char) {
    null, '\n' -> error("Missing Double quote")
    '"' -> traverser.pop().let { second ->
        if (second == '"') traverser.pop().let { third ->
            if (second == '"') StringTemplateToken(if (builder.isEmpty()) parts else parts + StringPart(builder)) to traverser.pop()
            else handleMultiLineString(traverser, third, parts, builder.append("\"\""))
        }
        else handleMultiLineString(traverser, second, parts, builder.append('"'))
    }
    '$' -> traverser.pop().let { next ->
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val row = traverser.row
                val (identifier, newNext) = handleIdentifier(traverser, next)
                handleMultiLineString(traverser, newNext, parts + StringPart(builder) + TokensPart(identifier on row))
            }
            next == '{' -> {
                val stack = TokenList()
                handleInterpolation(traverser, traverser.pop(), stack)
                handleMultiLineString(traverser, traverser.pop(), parts + StringPart(builder) + TokensPart(stack))
            }
            else -> handleMultiLineString(traverser, next, parts, builder.append(char))
        }
    }
    else -> handleMultiLineString(traverser, traverser.pop(), parts, builder.append(char))
}

fun handleInterpolation(traverser: StringTraverser, char: Char?, list: TokenList): Unit = when {
    char == null -> error("Open interpolated expression")
    char == '}' -> Unit
    char.isWhitespace() -> handleInterpolation(traverser, traverser.pop(), list)
    char.isJavaIdentifierStart() -> {
        val row = traverser.row
        val (identifier, next) = handleIdentifier(traverser, char)
        list.offer(identifier on row)
        handleInterpolation(traverser, next, list)
    }
    char == '#' -> handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        handleInterpolation(traverser, next, list)
    } else {
        list.offer(OpenCurlyBracketToken on traverser.row)
        handleInterpolation(traverser, traverser.pop(), list)
        list.offer(ClosedCurlyBracketToken on traverser.row)
        handleInterpolation(traverser, traverser.pop(), list)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), list)
    } else {
        val row = traverser.row
        val (symbol, next) = handleSymbol(traverser, char)
        list.offer(symbol on row)
        handleInterpolation(traverser, next, list)
    }
    else -> {
        val row = traverser.row
        val (token, next) = handleToken(traverser, char, list)
        list.offer(token on row..traverser.lastRow)
        handleInterpolation(traverser, next, list)
    }
}