package com.scientianova.palm.tokenizer

tailrec fun handleSingleLineComment(traverser: StringTraverser, char: Char?): Char? = when (char) {
    null, '\n' -> char
    else -> handleSingleLineComment(traverser, traverser.pop())
}

fun handleMultiLineComment(traverser: StringTraverser, char: Char?): Char? = when (char) {
    null -> char
    ']' -> traverser.pop().let { next ->
        if (next == '#') char
        else handleMultiLineComment(traverser, next)
    }
    '#' -> traverser.pop().let { next ->
        if (next == '[') {
            val newNext = handleMultiLineComment(traverser, next)
            handleMultiLineComment(traverser, newNext)
        } else handleMultiLineComment(traverser, next)
    }
    else -> handleSingleLineComment(traverser, traverser.pop())
}