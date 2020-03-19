package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_ESCAPE_CHARACTER_ERROR
import com.scientianova.palm.errors.MISSING_DOUBLE_QUOTE_ERROR
import com.scientianova.palm.errors.UNCLOSED_INTERPOLATED_EXPRESSION_ERROR
import com.scientianova.palm.errors.UNCLOSED_MULTILINE_STRING
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import java.util.*

fun handleSingleLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    parts: List<StringTokenPart> = emptyList(),
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<StringToken>, Char?> = when (char) {
    null, '\n' -> traverser.error(MISSING_DOUBLE_QUOTE_ERROR, traverser.lastPos)
    '"' ->
        (if (parts.isEmpty()) PureStringToken(builder.toString())
        else StringTemplateToken(if (builder.isEmpty()) parts else parts + StringPart(builder))) on
                startPos..traverser.lastPos to traverser.pop()
    '$' -> traverser.pop().let { next ->
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val (identifier, newNext) = handleIdentifier(traverser, next)
                handleSingleLineString(
                    traverser, newNext, startPos, parts + StringPart(builder) + TokensPart(identifier)
                )
            }
            next == '{' -> {
                val stack = LinkedList<PositionedToken>()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleSingleLineString(
                    traverser, traverser.pop(), startPos, parts + StringPart(builder) + TokensPart(stack)
                )
            }
            else -> handleSingleLineString(traverser, next, startPos, parts, builder.append(char))
        }
    }
    '\\' -> handleSingleLineString(
        traverser, traverser.pop(), startPos, parts,
        builder.append(
            handleEscaped(traverser, traverser.pop())
                ?: traverser.error(INVALID_ESCAPE_CHARACTER_ERROR, traverser.lastPos)
        )
    )
    else -> handleSingleLineString(traverser, traverser.pop(), startPos, parts, builder.append(char))
}

fun handleMultiLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    parts: List<StringTokenPart> = emptyList(),
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<StringTemplateToken>, Char?> = when (char) {
    null -> traverser.error(UNCLOSED_MULTILINE_STRING, startPos..startPos.shift(3))
    '"' -> traverser.pop().let { second ->
        if (second == '"') traverser.pop().let { third ->
            if (second == '"')
                StringTemplateToken(if (builder.isEmpty()) parts else parts + StringPart(builder)) on
                        startPos..traverser.lastPos to traverser.pop()
            else handleMultiLineString(traverser, third, startPos, parts, builder.append("\"\""))
        }
        else handleMultiLineString(traverser, second, startPos, parts, builder.append('"'))
    }
    '$' -> traverser.pop().let { next ->
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val (identifier, newNext) = handleIdentifier(traverser, next)
                handleMultiLineString(
                    traverser, newNext, startPos, parts + StringPart(builder) + TokensPart(identifier)
                )
            }
            next == '{' -> {
                val stack = TokenList()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleMultiLineString(
                    traverser, traverser.pop(), startPos, parts + StringPart(builder) + TokensPart(stack)
                )
            }
            else -> handleMultiLineString(traverser, next, startPos, parts, builder.append(char))
        }
    }
    else -> handleMultiLineString(traverser, traverser.pop(), startPos, parts, builder.append(char))
}

fun handleInterpolation(
    traverser: StringTraverser,
    char: Char?,
    list: TokenList,
    startPos: StringPos
): Unit = when {
    char == null -> traverser.error(UNCLOSED_INTERPOLATED_EXPRESSION_ERROR, startPos)
    char == '}' -> Unit
    char.isWhitespace() -> handleInterpolation(traverser, traverser.pop(), list, startPos)
    char.isJavaIdentifierStart() -> {
        val (identifier, next) = handleIdentifier(traverser, char)
        list.offer(identifier)
        handleInterpolation(traverser, next, list, startPos)
    }
    char == '#' -> handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), list, startPos)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        handleInterpolation(traverser, next, list, startPos)
    } else {
        list.offer(OpenCurlyBracketToken on traverser.lastPos)
        handleInterpolation(traverser, traverser.pop(), list, startPos)
        list.offer(ClosedCurlyBracketToken on traverser.lastPos)
        handleInterpolation(traverser, traverser.pop(), list, startPos)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), list, startPos)
    } else {
        val (token, next) = handleMisc(traverser, char, list)
        list.offer(token)
        handleInterpolation(traverser, next, list, startPos)
    }
    else -> {
        val (token, next) = handleToken(traverser, char, list)
        list.offer(token)
        handleInterpolation(traverser, next, list, startPos)
    }
}