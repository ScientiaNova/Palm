package com.scientianova.palm

import com.scientianova.palm.parser.Parser
import com.scientianova.palm.parser.handleExpression
import com.scientianova.palm.tokenizer.tokenize

fun main() {
    val code = "[n for nested in list for n in nested] where { list = [2 4; 5 10] }"
    val parser = Parser(tokenize(code), code)
    println(handleExpression(parser, parser.pop()).first.value.evaluate())
}