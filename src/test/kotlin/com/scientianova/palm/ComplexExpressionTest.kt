package com.scientianova.palm

import com.scientianova.palm.parser.handleExpression
import com.scientianova.palm.tokenizer.tokenize
import com.scientianova.palm.util.flip

fun main() {
    val tokens = tokenize("[n for nested in list for n in nested] where { list = [2 4; 5 10] }").flip()
    println(handleExpression(tokens, tokens.pop()).first.value.evaluate())
}