package com.scientianovateam.palm.tokenizer

fun handleChar(traverser: StringTraverser, char: Char?): Pair<CharToken, Char?> {
    val (value, end) = when (char) {
        null -> error("Lone single quote")
        '\\' -> handleEscaped(traverser, traverser.pop())
        else -> char to traverser.pop()
    }
    return if (end == '\'') CharToken(value) to traverser.pop()
    else error("Missing Single Quote")
}

fun handleEscaped(traverser: StringTraverser, char: Char?) = when (char) {
    '"' -> '\"' to traverser.pop()
    '$' -> '$' to traverser.pop()
    '\\' -> '\\' to traverser.pop()
    't' -> '\t' to traverser.pop()
    'n' -> '\n' to traverser.pop()
    'b' -> '\b' to traverser.pop()
    'r' -> '\r' to traverser.pop()
    'f' -> 12.toChar() to traverser.pop()
    'v' -> 11.toChar() to traverser.pop()
    'u' -> handleUnicode(traverser, traverser.pop())
    else -> error("Unknown escape character: $char")
}

fun handleUnicode(
    traverser: StringTraverser,
    char: Char?,
    idBuilder: StringBuilder = StringBuilder()
): Pair<Char, Char?> = if (char in '0'..'9') {
    idBuilder.append(char)
    if (idBuilder.length == 4) idBuilder.toString().toInt().toChar() to traverser.pop()
    else handleUnicode(traverser, traverser.pop(), idBuilder)
} else idBuilder.toString().toInt().toChar() to char