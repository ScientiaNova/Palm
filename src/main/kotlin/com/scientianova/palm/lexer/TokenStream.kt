package com.scientianova.palm.lexer

class TokenStream(code: String) {
    private val iterator = TokenIterator(code)
    private val list = mutableListOf<PToken>()

    operator fun get(index: Int): PToken {
        while (index <= list.size) {
            if (!iterator.hasNext()) return list.last()
            list += iterator.next()
        }
        return list[index]
    }
}