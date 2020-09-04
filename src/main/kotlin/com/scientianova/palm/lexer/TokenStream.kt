package com.scientianova.palm.lexer

class TokenStream(code: String) {
    private val iterator = TokenIterator(code)
    private val list = mutableListOf<PToken>()

    operator fun get(index: Int): PToken = if (index >= list.size) {
        val next = iterator.next()
        list += next
        next
    } else {
        list[index]
    }
}