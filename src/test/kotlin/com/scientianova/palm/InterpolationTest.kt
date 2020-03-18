package com.scientianova.palm

import com.scientianova.palm.parser.handleExpression
import com.scientianova.palm.tokenizer.tokenize

fun main() {
    val tokens = tokenize("\"\${x[1]}\" where { x = [1 5 10] }")
    println(handleExpression(tokens, tokens.poll()).first.value.evaluate())
}