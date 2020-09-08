package com.scientianova.palm.parser

import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.TokenStream

class Parser(private val stream: TokenStream, private var index: Int) {
    private var trackNewline: Boolean = true

    val current get() = stream[index]

    val next get() = stream[index + 1]

    fun advance(): Parser = this.also { advance(index + 1) }

    private tailrec fun advance(newIndex: Int): Unit = if (stream[newIndex].value.canIgnore()) {
        advance(newIndex + 1)
    } else {
        index = newIndex
    }

    fun lastNewline() = trackNewline && lastNewLine(index - 1)

    private tailrec fun lastNewLine(index: Int): Boolean {
        val token = stream[index].value
        return when {
            token == Token.EOL -> true
            token.canIgnore() -> lastNewLine(index - 1)
            else -> false
        }
    }

    fun disableNewline() {
        trackNewline = false
    }

    fun rawLookup(offset: Int) = stream[index + offset]

    inner class Marker {
        private val index = this@Parser.index
        private val trackNewline = this@Parser.trackNewline

        fun revertIndex() {
            this@Parser.index = index
        }

        fun revertNewline() {
            this@Parser.trackNewline = trackNewline
        }
    }
}

fun <T> T.alsoAdvance(parser: Parser) = this.also { parser.advance() }