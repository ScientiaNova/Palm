package com.scientianova.palm.parser

import com.scientianova.palm.parser.parsing.top.parseFile

inline fun <T> testParse(code: String, fn: (Parser) -> T): T {
    val parser = parserFor(code)
    return fn(parser).also { parser.errors.forEach(::println) }
}

fun parseTestCode(code: String) = testParse(code, Parser::parseFile)