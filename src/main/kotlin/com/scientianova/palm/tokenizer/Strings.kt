package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.on

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
                val stack = TokenStack()
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
                val stack = TokenStack()
                handleInterpolation(traverser, traverser.pop(), stack)
                handleMultiLineString(traverser, traverser.pop(), parts + StringPart(builder) + TokensPart(stack))
            }
            else -> handleMultiLineString(traverser, next, parts, builder.append(char))
        }
    }
    else -> handleMultiLineString(traverser, traverser.pop(), parts, builder.append(char))
}

fun handleInterpolation(traverser: StringTraverser, char: Char?, stack: TokenStack): Unit = when {
    char == null -> error("Open interpolated expression")
    char == '}' -> Unit
    char.isWhitespace() -> handleInterpolation(traverser, traverser.pop(), stack)
    char.isJavaIdentifierStart() -> {
        val row = traverser.row
        val (identifier, next) = handleIdentifier(traverser, char)
        stack.push(identifier on row)
        handleInterpolation(traverser, next, stack)
    }
    char == '#' -> handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), stack)
    char == '{' -> if (traverser.peek() == '-') {
        traverser.pop()
        val next = handleMultiLineComment(traverser, traverser.pop())
        handleInterpolation(traverser, next, stack)
    } else {
        stack.push(OpenCurlyBracketToken on traverser.row)
        handleInterpolation(traverser, traverser.pop(), stack)
        stack.push(ClosedCurlyBracketToken on traverser.row)
        handleInterpolation(traverser, traverser.pop(), stack)
    }
    char == '-' -> if (traverser.peek() == '-') {
        traverser.pop()
        handleInterpolation(traverser, handleSingleLineComment(traverser, traverser.pop()), stack)
    } else {
        val row = traverser.row
        val (symbol, next) = handleSymbol(traverser, char)
        stack.push(symbol on row)
        handleInterpolation(traverser, next, stack)
    }
    else -> {
        val row = traverser.row
        val (token, next) = handleToken(traverser, char, stack)
        stack.push(token on row..traverser.lastRow)
        handleInterpolation(traverser, next, stack)
    }
}