package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

class TokenStream(code: String, startPos: StringPos = 0) {
    private val iterator = TokenIterator(code, startPos)
    private val tokens = mutableListOf<Token>()
    private val positions = mutableListOf(startPos)

    fun getToken(index: Int): Token {
        while (index >= tokens.size) {
            if (!iterator.hasNext()) return tokens.last()
            val (token, nextPos) = iterator.next()
            tokens.add(token)
            positions.add(nextPos)
        }
        return tokens[index]
    }

    fun getPos(index: Int): Int {
        while (index >= tokens.size) {
            if (!iterator.hasNext()) return positions.last()
            val (token, nextPos) = iterator.next()
            tokens.add(token)
            positions.add(nextPos)
        }
        return positions[index]
    }
}