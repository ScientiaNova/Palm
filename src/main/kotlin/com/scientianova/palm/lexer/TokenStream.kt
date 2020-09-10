package com.scientianova.palm.lexer

class TokenStream(code: String) {
    private val iterator = TokenIterator(code)
    private val tokens = mutableListOf<Token>()
    private val positions = mutableListOf(0)

    fun getToken(index: Int): Token {
        while (index <= tokens.size) {
            if (!iterator.hasNext()) return tokens.last()
            val (token, nextPos) = iterator.next()
            tokens.add(token)
            positions.add(nextPos)
        }
        return tokens[index]
    }

    fun getPos(index: Int): Int {
        while (index <= tokens.size) {
            if (!iterator.hasNext()) return positions.last()
            val (token, nextPos) = iterator.next()
            tokens.add(token)
            positions.add(nextPos)
        }
        return positions[index]
    }
}