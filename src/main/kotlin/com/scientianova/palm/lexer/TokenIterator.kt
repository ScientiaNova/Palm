package com.scientianova.palm.lexer

class TokenIterator(private val code: String) : Iterator<TokenData> {
    private var pos = 0

    override fun hasNext() = pos <= code.length

    override fun next(): TokenData {
        val data = lex(code, pos)
        pos = data.nextPos
        return data
    }
}

fun <T> Iterator<T>.toList() = ArrayList<T>().apply { while (hasNext()) add(next()) }