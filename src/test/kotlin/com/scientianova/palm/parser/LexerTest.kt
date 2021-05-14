package com.scientianova.palm.parser

import com.scientianova.palm.lexer.Lexer
import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.lexFile
import java.net.URL

fun testLex(path: URL): List<PToken> = Lexer(path.readText(), path, errors = mutableListOf()).also {
    it.lexFile()
    it.errors.forEach(::println)
}.tokens