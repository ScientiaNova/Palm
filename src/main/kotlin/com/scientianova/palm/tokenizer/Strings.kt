package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_ESCAPE_CHARACTER_ERROR
import com.scientianova.palm.errors.MISSING_DOUBLE_QUOTE_ERROR
import com.scientianova.palm.errors.UNCLOSED_INTERPOLATED_EXPRESSION_ERROR
import com.scientianova.palm.errors.UNCLOSED_MULTILINE_STRING
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import java.util.*

sealed class StringTokenPart
typealias PStringTokenPart = Positioned<StringTokenPart>

data class StringPart(val string: String) : StringTokenPart() {
    constructor(builder: StringBuilder) : this(builder.toString())
}

data class TokensPart(val tokens: TokenList) : StringTokenPart() {
    constructor(token: PToken) : this(TokenList().apply { offer(token) })
}

tailrec fun handleSingleLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    list: TokenList,
    parts: List<PStringTokenPart>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): Pair<PToken, Char?> = when (char) {
    null, '\n' -> traverser.error(MISSING_DOUBLE_QUOTE_ERROR, traverser.lastPos)
    '"' ->
        (if (parts.isEmpty()) PureStringToken(builder.toString())
        else StringTemplateToken(if (builder.isEmpty()) parts else parts + (StringPart(builder) on lastStart..traverser.lastPos))) on
                startPos..traverser.lastPos to traverser.pop()
    '$' -> {
        val next = traverser.pop()
        val interStart = traverser.lastPos
        when {
            next?.isJavaIdentifierStart() == true -> {
                val (identifier, newNext) = handleIdentifier(
                    traverser, next, next.isUpperCase(), list, traverser.lastPos, StringBuilder()
                )
                handleSingleLineString(
                    traverser, newNext, startPos, list,
                    parts + (StringPart(builder) on startPos..interStart) + (TokensPart(identifier) on interStart..traverser.lastPos),
                    StringBuilder(), traverser.lastPos.shift(rows = 1)
                )
            }
            next == '{' -> {
                val stack = LinkedList<PToken>()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleSingleLineString(
                    traverser, traverser.pop(), startPos, list,
                    parts + (StringPart(builder) on startPos..interStart) + (TokensPart(stack) on interStart..traverser.lastPos),
                    StringBuilder(), traverser.lastPos.shift(rows = 1)
                )
            }
            else -> handleSingleLineString(traverser, next, startPos, list, parts, builder.append(char), lastStart)
        }
    }
    '\\' -> handleSingleLineString(
        traverser, traverser.pop(), startPos, list, parts,
        builder.append(
            handleEscaped(traverser, traverser.pop())
                ?: traverser.error(INVALID_ESCAPE_CHARACTER_ERROR, traverser.lastPos)
        ), lastStart
    )
    else -> handleSingleLineString(traverser, traverser.pop(), startPos, list, parts, builder.append(char), lastStart)
}

tailrec fun handleMultiLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    list: TokenList,
    parts: List<PStringTokenPart>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): Pair<Positioned<StringTemplateToken>, Char?> = when (char) {
    null -> traverser.error(UNCLOSED_MULTILINE_STRING, startPos..startPos.shift(3))
    '"' -> {
        val second = traverser.pop()
        if (second == '"') {
            val third = traverser.pop()
            if (second == '"')
                StringTemplateToken(if (builder.isEmpty()) parts else parts + (StringPart(builder) on lastStart..traverser.lastPos)) on
                        startPos..traverser.lastPos to traverser.pop()
            else handleMultiLineString(traverser, third, startPos, list, parts, builder.append("\"\""), lastStart)
        } else handleMultiLineString(traverser, second, startPos, list, parts, builder.append('"'), lastStart)
    }
    '$' -> {
        val next = traverser.pop()
        val interStart = traverser.lastPos
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val (identifier, newNext) = handleIdentifier(
                    traverser, next, next.isUpperCase(), list, traverser.lastPos, StringBuilder()
                )
                handleMultiLineString(
                    traverser, newNext, startPos, list,
                    parts + (StringPart(builder) on startPos..interStart) + (TokensPart(identifier) on interStart..traverser.lastPos),
                    StringBuilder(), traverser.lastPos.shift(rows = 1)
                )
            }
            next == '{' -> {
                val stack = TokenList()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleMultiLineString(
                    traverser, traverser.pop(), startPos, list,
                    parts + (StringPart(builder) on startPos..interStart) + (TokensPart(stack) on interStart..traverser.lastPos),
                    StringBuilder(), traverser.lastPos.shift(rows = 1)
                )
            }
            else -> handleMultiLineString(traverser, next, startPos, list, parts, builder.append(char), lastStart)
        }
    }
    else -> handleMultiLineString(traverser, traverser.pop(), startPos, list, parts, builder.append(char), lastStart)
}

tailrec fun handleInterpolation(
    traverser: StringTraverser,
    char: Char?,
    list: TokenList,
    startPos: StringPos
): Unit = when {
    char == null -> traverser.error(UNCLOSED_INTERPOLATED_EXPRESSION_ERROR, startPos)
    char == '}' -> Unit
    char.isWhitespace() -> handleInterpolation(traverser, traverser.pop(), list, startPos)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(
            traverser, char, char.isUpperCase(), list, traverser.lastPos, StringBuilder()
        )
        list.offer(identifier)
        handleInterpolation(traverser, next, list, startPos)
    }
    char == '#' -> when (traverser.peek()) {
        '[' -> handleInterpolation(traverser, handleMultiLineComment(traverser, traverser.pop()), list, startPos)
        else -> handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), list, startPos)
    }
    else -> {
        val (token, next) = handleToken(traverser, char, list)
        list.offer(token)
        handleInterpolation(traverser, next, list, startPos)
    }
}