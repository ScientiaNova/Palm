package com.scientianova.palm.tokenizer

tailrec fun handleSingleLineComment(traverser: StringTraverser, char: Char?): Char? = when (char) {
    null, '\n' -> char
    else -> handleSingleLineComment(traverser, traverser.pop())
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleMultiLineComment(traverser: StringTraverser, char: Char?): Char? = when (char) {
    null -> char
    ']' ->  {
        val next = traverser.pop()
        if (next == '#') char
        else handleMultiLineComment(traverser, next)
    }
    '#' ->  {
        val next = traverser.pop()
        if (next == '[') {
            val newNext = handleMultiLineComment(traverser, next)
            handleMultiLineComment(traverser, newNext)
        } else handleMultiLineComment(traverser, next)
    }
    else -> handleSingleLineComment(traverser, traverser.pop())
}