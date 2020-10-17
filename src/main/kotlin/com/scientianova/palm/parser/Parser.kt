package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.messageFor
import com.scientianova.palm.errors.missingIdentifier
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.TokenStream
import com.scientianova.palm.parser.data.top.FileScope
import com.scientianova.palm.util.*

class Parser(private val stream: TokenStream) {
    private var index = nextIndex(0)

    var trackNewline = true
    var excludeCurly = false

    val current get() = stream.getToken(index)
    val pos get() = stream.getPos(index)
    val nextPos get() = stream.getPos(index + 1)

    fun <T> end(thing: T, startIndex: Int = index - 1) = Positioned(thing, stream.getPos(startIndex), pos)

    fun err(error: PalmError): Nothing {
        val current = current
        if (current is Token.Error) {
            parseErr(current.error)
        } else {
            parseErr(error, pos, nextPos)
        }
    }

    fun err(error: PalmError, startIndex: Int): Nothing = parseErr(error, stream.getPos(startIndex), pos)

    fun advance(): Parser = this.also { index = nextIndex(index + 1) }

    private tailrec fun nextIndex(newIndex: Int): Int = if (stream.getToken(newIndex).canIgnore()) {
        nextIndex(newIndex + 1)
    } else {
        newIndex
    }

    fun revertIndex(marker: ParseMarker) {
        index = marker.index
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

    fun mark() = ParseMarker(this, index)

    inline fun <T> withFlags(trackNewline: Boolean, excludeCurly: Boolean, crossinline body: () -> T): T {
        val startNewLine = this.trackNewline
        val startCurly = this.excludeCurly

        this.trackNewline = trackNewline
        this.excludeCurly = excludeCurly

        val res = body()

        this.trackNewline = startNewLine
        this.excludeCurly = startCurly

        return res
    }
}

data class ParseMarker(
    val parser: Parser,
    val index: Int
) {
    fun <T> end(thing: T) = parser.end(thing, index)

    fun err(error: PalmError): Nothing = parser.err(error, index)

    fun revertIndex() = parser.revertIndex(this)
}

inline fun <T> recBuildList(list: MutableList<T> = mutableListOf(), builder: MutableList<T>.() -> Any): List<T> {
    while (true) builder(list)
}

inline fun <T> recBuildListN(list: MutableList<T> = mutableListOf(), builder: MutableList<T>.() -> Any): Nothing {
    while (true) builder(list)
}

fun parseIdent(parser: Parser): PString {
    val ident = parser.current.identString()
    if (ident.isEmpty()) parser.err(missingIdentifier)
    return parser.advance().end(ident)
}

fun parseFile(code: String, name: String): Either<String, FileScope> = try {
    Right(com.scientianova.palm.parser.parsing.top.parseFile(Parser(TokenStream((code)))))
} catch (e: ParseException) {
    Left(e.error.messageFor(code, name))
}