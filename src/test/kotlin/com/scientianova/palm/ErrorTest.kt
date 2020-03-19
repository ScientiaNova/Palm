package com.scientianova.palm

import com.scientianova.palm.tokenizer.tokenize

fun main() {
    tokenize("""
{
    matrix = [4 7; 6 9; 8 18]
}
    """.trimIndent())
}