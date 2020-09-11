package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.missingIdentifier
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.TokenStream
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned

class Parser(private val stream: TokenStream) {
    private var index = nextIndex(0)

    var trackNewline = true
    var excludeCurly = false

    val current get() = stream.getToken(index)
    val pos get() = stream.getPos(index)
    val nextPos get() = stream.getPos(index + 1)

    fun <T> end(thing: T) = Positioned(thing, stream.getPos(index - 1), pos)

    fun err(error: PalmError): Nothing {
        val current = current
        if (current is Token.Error) {
            parseErr(current.error)
        } else {
            parseErr(error, pos, nextPos)
        }
    }

    fun advance(): Parser = this.also { index = nextIndex(index + 1) }

    private tailrec fun nextIndex(newIndex: Int): Int = if (stream.getToken(newIndex).canIgnore()) {
        nextIndex(newIndex + 1)
    } else {
        newIndex
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

inline fun <T> recBuildList(list: MutableList<T> = mutableListOf(), builder: MutableList<T>.() -> Unit): Nothing {
    while (true) builder(list)
}

fun parseIdent(parser: Parser): PString {
    val ident = parser.current.identString()
    if (ident.isEmpty()) parser.err(missingIdentifier)
    return parser.advance().end(ident)
}