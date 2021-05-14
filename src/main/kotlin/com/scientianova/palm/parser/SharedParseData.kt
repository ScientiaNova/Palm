package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.lexer.Lexer
import com.scientianova.palm.lexer.lexFile
import java.io.File
import java.net.URL

@JvmInline
value class SharedParseData(val errors: MutableList<PalmError>) {
    fun fileParser(file: File): Parser {
        val path = file.toURI().toURL()
        val lexer = Lexer(file.readText(), path, errors = errors)
        lexer.lexFile()
        return ScopedParser(path, lexer.tokens, errors)
    }

    fun fileParser(path: URL): Parser {
        val lexer = Lexer(path.readText(), path, errors = errors)
        lexer.lexFile()
        return ScopedParser(path, lexer.tokens, errors)
    }

    fun err(message: String, file: File) {
        errors += PalmError(message, file.toURI().toURL(), -1, -1)
    }
}