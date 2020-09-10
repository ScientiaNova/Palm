package com.scientianova.palm.lexer

class TokenIterator(private val code: String) : Iterator<PToken> {
    private var pos = 0

    override fun hasNext() = pos <= code.length

    override fun next(): PToken {
        val token = lex(code, pos)
        pos = token.second
        return token
    }
}

fun <T> Iterator<T>.toList() = ArrayList<T>().apply { while (hasNext()) add(next()) }