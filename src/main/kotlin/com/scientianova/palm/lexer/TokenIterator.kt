package com.scientianova.palm.lexer

class TokenIterator(private val code: String) : Iterator<PToken> {
    private var pos = 0

    override fun hasNext() = pos <= code.length

    override fun next(): PToken {
        val data = lex(code, pos)
        pos = data.next
        return data
    }
}

fun <T> Iterator<T>.toList() = ArrayList<T>().apply { while (hasNext()) add(next()) }