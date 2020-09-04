package com.scientianova.palm.parser

import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.TokenStream

class Parser(private val stream: TokenStream, index: Int) {
    var current = stream[index]
        private set

    var index = index
        private set

    fun atIndex(index: Int) = Parser(stream, index)

    fun copy() = Parser(stream, index)

    fun moveWithEOL(): PToken {
        index += 1
        current = stream[index]
        return current
    }

    fun move() = move(index + 1)

    private tailrec fun move(newIndex: Int): PToken {
        val token = stream[newIndex]
        return if (token.value is Token.EOL) {
            move(newIndex + 1)
        } else {
            current = token
            index = newIndex
            token
        }
    }
}