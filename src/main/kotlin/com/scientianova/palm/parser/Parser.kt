package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.lexer.Lexer
import com.scientianova.palm.lexer.PToken
import com.scientianova.palm.lexer.Token
import com.scientianova.palm.lexer.lexFile
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos

sealed class Parser(protected val stream: List<PToken>, val errors: MutableList<PalmError>) {
    constructor(lexer: Lexer) : this(lexer.tokens, lexer.errors)

    var index = nextIndex(0)

    val currentWithPos get() = stream[index]
    val current get() = currentWithPos.value
    val pos get() = currentWithPos.start
    val nextPos get() = currentWithPos.next
    val next get() = stream[nextIndex(index + 1)].value

    fun <T> T.end(startPos: Int = pos) = Positioned(this, startPos, nextPos).also { advance() }

    fun advance(): Parser = also { index = nextIndex(index + 1) }

    fun <T> T.noPos(repeat: StringPos = pos) = Positioned(this, repeat, repeat)

    fun err(error: PalmError): Parser = also { errors += error }
    fun err(error: String, startPos: StringPos = pos, nextPos: StringPos = this.nextPos): Parser =
        also { errors += PalmError(error, startPos, nextPos) }

    private tailrec fun nextIndex(newIndex: Int): Int = if (stream[newIndex].value.canIgnore()) {
        nextIndex(newIndex + 1)
    } else {
        newIndex
    }

    abstract val lastNewline: Boolean

    fun rawLookup(offset: Int) = stream[index + offset].value

    inline fun <T> withPos(fn: (StringPos) -> T): T = fn(pos)

    fun currentPostfix() = (stream.getOrNull(index - 1)?.let { !it.value.beforePrefix() } ?: false)
            && stream[index + 1].value.afterPostfix()

    fun currentPrefix() = (stream.getOrNull(index - 1)?.value?.beforePrefix() ?: true)
            && !stream[index + 1].value.afterPostfix()

    fun currentInfix() =
        (stream.getOrNull(index - 1)?.value?.beforePrefix() ?: true) == stream[index + 1].value.afterPostfix()

    fun parenthesizedOf(stream: List<PToken>, errors: MutableList<PalmError> = this.errors) = ParenthesizedParser(stream, errors)
    fun scopedOf(stream: List<PToken>, errors: MutableList<PalmError> = this.errors) = ScopedParser(stream, errors)

    inline fun <T> inParensOrEmpty(errors: MutableList<PalmError> = this.errors, crossinline fn: Parser.() -> List<T>) = current.let { paren ->
        if (paren is Token.Parens)
            parenthesizedOf(paren.tokens, errors).fn().also { advance() }
        else emptyList()
    }

    inline fun <T> inBracesOrEmpty(errors: MutableList<PalmError> = this.errors, crossinline fn: Parser.() -> List<T>) = current.let { brace ->
        if (brace is Token.Braces)
            scopedOf(brace.tokens, errors).fn().also { advance() }
        else emptyList()
    }

    inline fun <T> inBracketsOrEmpty(errors: MutableList<PalmError>, crossinline fn: Parser.() -> List<T>) = current.let { bracket ->
        if (bracket is Token.Brackets)
            parenthesizedOf(bracket.tokens, errors).fn().also { advance() }
        else emptyList()
    }

    inline fun <T> inBracketsOrEmpty(crossinline fn: Parser.() -> List<T>) = inBracketsOrEmpty(this.errors, fn)

    inline fun <T> inParensOr(crossinline fn: Parser.() -> T, or: () -> T) = current.let { paren ->
        if (paren is Token.Parens)
            parenthesizedOf(paren.tokens).fn().also { advance() }
        else or()
    }

    inline fun <T> inParensOr(errors: MutableList<PalmError>, crossinline fn: Parser.() -> T, or: () -> T) = current.let { paren ->
        if (paren is Token.Parens)
            parenthesizedOf(paren.tokens, errors).fn().also { advance() }
        else or()
    }

    inline fun <T> inBracesOr(crossinline fn: Parser.() -> T, or: () -> T) = current.let { brace ->
        if (brace is Token.Braces)
            scopedOf(brace.tokens).fn().also { advance() }
        else or()
    }

    inline fun <T> inBracketsOr(crossinline fn: Parser.() -> T, or: () -> T) = current.let { bracket ->
        if (bracket is Token.Brackets)
            parenthesizedOf(bracket.tokens).fn().also { advance() }
        else or()
    }
}

class ParenthesizedParser(stream: List<PToken>, errors: MutableList<PalmError>) : Parser(stream, errors) {
    override val lastNewline get() = false
}

class ScopedParser(stream: List<PToken>, errors: MutableList<PalmError>) : Parser(stream, errors) {
    override val lastNewline get() = lastNewLine(index - 1)

    private tailrec fun lastNewLine(index: Int): Boolean {
        val token = stream[index].value
        return when {
            token == Token.EOL -> true
            token.canIgnore() -> lastNewLine(index - 1)
            else -> false
        }
    }
}

fun parserFor(code: String): Parser {
    val lexer = Lexer()
    lexer.lexFile(code)
    return ScopedParser(lexer.tokens, lexer.errors)
}

fun Parser.parseIdent(): PString {
    val curr = current
    return if (curr is Token.Ident) {
        curr.name.end()
    } else {
        err("Missing identifier")
        "".noPos()
    }
}