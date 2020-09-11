package com.scientianova.palm.parser

import com.scientianova.palm.lexer.TokenIterator
import com.scientianova.palm.lexer.toList

fun testLex(code: String) = TokenIterator(code).toList()

fun testLexString(code: String) = testLex(code).joinToString("\n") { it.first.toCodeString() }