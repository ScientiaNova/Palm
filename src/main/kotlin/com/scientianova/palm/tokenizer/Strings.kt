package com.scientianova.palm.tokenizer

import com.scientianova.palm.errors.INVALID_ESCAPE_CHARACTER_ERROR
import com.scientianova.palm.errors.MISSING_DOUBLE_QUOTE_ERROR
import com.scientianova.palm.errors.UNCLOSED_INTERPOLATED_EXPRESSION_ERROR
import com.scientianova.palm.errors.UNCLOSED_MULTILINE_STRING
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import java.util.*

sealed class StringToken : IToken
data class PureStringToken(override val name: String) : StringToken(), IKeyToken
data class StringTemplateToken(val parts: List<StringTokenPart>) : StringToken()

sealed class StringTokenPart
data class StringPart(val string: String) : StringTokenPart() {
    constructor(builder: StringBuilder) : this(builder.toString())
}

data class TokensPart(val tokens: TokenList) : StringTokenPart() {
    constructor(token: PositionedToken) : this(TokenList().apply { offer(token) })
}

fun handleSingleLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    list: TokenList,
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
                val (identifier, newNext) = handleIdentifier(traverser, next, list)
                handleSingleLineString(
                    traverser, newNext, startPos, list, parts + StringPart(builder) + TokensPart(identifier)
                )
            }
            next == '{' -> {
                val stack = LinkedList<PositionedToken>()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleSingleLineString(
                    traverser, traverser.pop(), startPos, list, parts + StringPart(builder) + TokensPart(stack)
                )
            }
            else -> handleSingleLineString(traverser, next, startPos, list, parts, builder.append(char))
        }
    }
    '\\' -> handleSingleLineString(
        traverser, traverser.pop(), startPos, list, parts,
        builder.append(
            handleEscaped(traverser, traverser.pop())
                ?: traverser.error(INVALID_ESCAPE_CHARACTER_ERROR, traverser.lastPos)
        )
    )
    else -> handleSingleLineString(traverser, traverser.pop(), startPos, list, parts, builder.append(char))
}

fun handleMultiLineString(
    traverser: StringTraverser,
    char: Char?,
    startPos: StringPos,
    list: TokenList,
    parts: List<StringTokenPart> = emptyList(),
    builder: StringBuilder = StringBuilder()
): Pair<Positioned<StringTemplateToken>, Char?> = when (char) {
    null -> traverser.error(UNCLOSED_MULTILINE_STRING, startPos..startPos.shift(3))
    '"' -> traverser.pop().let { second ->
        if (second == '"') traverser.pop().let { third ->
            if (second == '"')
                StringTemplateToken(if (builder.isEmpty()) parts else parts + StringPart(builder)) on
                        startPos..traverser.lastPos to traverser.pop()
            else handleMultiLineString(traverser, third, startPos, list, parts, builder.append("\"\""))
        }
        else handleMultiLineString(traverser, second, startPos, list, parts, builder.append('"'))
    }
    '$' -> traverser.pop().let { next ->
        when {
            next?.isJavaIdentifierStart() == true && next.isLowerCase() -> {
                val (identifier, newNext) = handleIdentifier(traverser, next, list)
                handleMultiLineString(
                    traverser, newNext, startPos, list, parts + StringPart(builder) + TokensPart(identifier)
                )
            }
            next == '{' -> {
                val stack = TokenList()
                val bracketPos = traverser.lastPos
                handleInterpolation(traverser, traverser.pop(), stack, bracketPos)
                handleMultiLineString(
                    traverser, traverser.pop(), startPos, list, parts + StringPart(builder) + TokensPart(stack)
                )
            }
            else -> handleMultiLineString(traverser, next, startPos, list, parts, builder.append(char))
        }
    }
    else -> handleMultiLineString(traverser, traverser.pop(), startPos, list, parts, builder.append(char))
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
        val (identifier, next) = handleIdentifier(traverser, char, list)
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