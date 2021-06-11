package com.palmlang.palm.parser

import com.palmlang.palm.lexer.Lexer
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.lexFile
import java.net.URL

fun testLex(path: URL): List<PToken> = Lexer(path.readText(), path, errors = mutableListOf()).also {
    it.lexFile()
    it.errors.forEach(::println)
}.tokens