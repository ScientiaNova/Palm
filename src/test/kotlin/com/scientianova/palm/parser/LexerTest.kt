package com.scientianova.palm.parser

import com.scientianova.palm.lexer.*

fun testLex(code: String): List<PToken> {
    val lexer = Lexer()
    lexer.lexFile(code)
    lexer.errors.forEach(::println)
    return lexer.tokens
}

fun testLexString(code: String) = testLex(code).joinToString("\n") { it.value.toCodeString(0) }