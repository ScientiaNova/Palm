package com.scientianova.palm.parser

import com.scientianova.palm.lexer.TokenStream

inline fun <T> testParse(code: String, fn: (Parser) -> T) = fn(Parser(TokenStream(code)))