package com.scientianova.palm.lexer

import com.scientianova.palm.util.StringPos

class TokenIterator(private val code: String, private var pos: StringPos = 0) : Iterator<PToken> {
    override fun hasNext() = pos <= code.length

    override fun next(): PToken {
        val token = lex(code, pos)
        pos = token.second
        return token
    }
}

fun <T> Iterator<T>.toList() = ArrayList<T>().apply { while (hasNext()) add(next()) }