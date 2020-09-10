package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.TokenStream
import com.scientianova.palm.util.Positioned

class Parser(private val stream: TokenStream, private var index: Int) {
    var trackNewline = true
    var excludeCurly = false

    val current get() = stream.getToken(index)
    val pos get() = stream.getPos(index)

    fun <T> end(thing: T) = Positioned(thing, stream.getPos(index - 1), pos)

    fun err(error: PalmError): Nothing = parseErr(error, stream.getPos(index - 1), pos)

    fun advance(): Parser = this.also { advance(index + 1) }

    private tailrec fun advance(newIndex: Int): Unit = if (stream.getToken(newIndex).canIgnore()) {
        advance(newIndex + 1)
    } else {
        index = newIndex
    }

    val lastNewline get() = trackNewline && lastNewLine(index - 1)

    private tailrec fun lastNewLine(index: Int): Boolean {
        val token = stream.getToken(index)
        return when {
            token == Token.EOL -> true
            token.canIgnore() -> lastNewLine(index - 1)
            else -> false
        }
    }

    fun rawLookup(offset: Int) = stream.getToken(index + offset)

    inner class Marker {
        private val index = this@Parser.index
        private val trackNewline = this@Parser.trackNewline
        private val excludeCurly = this@Parser.excludeCurly

        fun <T> end(thing: T) = Positioned(thing, stream.getPos(index), pos)

        fun err(error: PalmError): Nothing = parseErr(error, stream.getPos(index), pos)

        fun revertIndex() {
            this@Parser.index = index
        }

        fun revertFlags() {
            this@Parser.trackNewline = trackNewline
            this@Parser.excludeCurly = excludeCurly
        }
    }
}

inline fun <T> recBuildList(builder: MutableList<T>.() -> Unit): Nothing {
    val list = mutableListOf<T>()
    while (true) builder(list)
}