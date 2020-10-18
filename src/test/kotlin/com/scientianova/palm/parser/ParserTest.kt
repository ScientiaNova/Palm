package com.scientianova.palm.parser

import com.scientianova.palm.lexer.TokenStream
import com.scientianova.palm.parser.data.top.FileScope
import com.scientianova.palm.util.Either
import com.scientianova.palm.util.Left
import com.scientianova.palm.util.Right

inline fun <T> testParse(code: String, fn: (Parser) -> T) = fn(Parser(TokenStream(code)))

fun parseTestCode(code: String) = parseFile(code, "Test")

fun Either<String, FileScope>.print() {
    println(
        when (this) {
            is Left -> left
            is Right -> right.toCodeString(0)
        }
    )
    readLine()
}