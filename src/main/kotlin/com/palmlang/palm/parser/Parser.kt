package com.palmlang.palm.parser

import com.palmlang.palm.errors.PalmError
import com.palmlang.palm.lexer.Lexer
import com.palmlang.palm.lexer.PToken
import com.palmlang.palm.lexer.Token
import com.palmlang.palm.util.PString
import com.palmlang.palm.util.Positioned
import com.palmlang.palm.util.StringPos
import com.palmlang.palm.util.recBuildListN
import java.net.URL

sealed class Parser(private val filePath: URL, protected val stream: List<PToken>, val errors: MutableList<PalmError>) {
    constructor(lexer: Lexer) : this(lexer.filePath, lexer.tokens, lexer.errors)

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
        also { errors += PalmError(error, filePath, startPos, nextPos) }

    private tailrec fun nextIndex(newIndex: Int): Int =
        if (stream[newIndex].value.canIgnore()) nextIndex(newIndex + 1) else newIndex

    private tailrec fun lastIndex(oldIndex: Int): Int =
        if (oldIndex != 0 && stream[oldIndex - 1].value.canIgnore()) lastIndex(oldIndex - 1) else oldIndex

    val lastPos = stream[lastIndex(index)].start

    abstract val lastNewline: Boolean

    fun rawLookup(offset: Int) = stream[index + offset].value

    inline fun <T> withPos(fn: (StringPos) -> T): T = fn(pos)

    fun currentPostfix() = (stream.getOrNull(index - 1)?.let { !it.value.beforePrefix() } ?: false)
            && stream[index + 1].value.afterPostfix()

    fun currentPrefix() = (stream.getOrNull(index - 1)?.value?.beforePrefix() ?: true)
            && !stream[index + 1].value.afterPostfix()

    fun currentInfix() =
        (stream.getOrNull(index - 1)?.value?.beforePrefix() ?: true) == stream[index + 1].value.afterPostfix()

    fun parenthesizedOf(stream: List<PToken>, errors: MutableList<PalmError> = this.errors) =
        ParenthesizedParser(filePath, stream, errors)

    fun scopedOf(stream: List<PToken>, errors: MutableList<PalmError> = this.errors) =
        ScopedParser(filePath, stream, errors)

    inline fun <T> inParensOrEmpty(errors: MutableList<PalmError> = this.errors, crossinline fn: Parser.() -> List<T>) =
        current.let { paren ->
            if (paren is Token.Parens)
                parenthesizedOf(paren.tokens, errors).fn().also { advance() }
            else emptyList()
        }

    inline fun <T> inBracesOrEmpty(errors: MutableList<PalmError> = this.errors, crossinline fn: Parser.() -> List<T>) =
        current.let { brace ->
            if (brace is Token.Braces)
                scopedOf(brace.tokens, errors).fn().also { advance() }
            else emptyList()
        }

    inline fun <T> inBracketsOrEmpty(errors: MutableList<PalmError>, crossinline fn: Parser.() -> List<T>) =
        current.let { bracket ->
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

    inline fun <T> inParensOr(errors: MutableList<PalmError>, crossinline fn: Parser.() -> T, or: () -> T) =
        current.let { paren ->
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

    inline fun <T, R> parseCommaSeparated(fn: Parser.() -> T, toRes: (List<T>, Boolean) -> R): R = recBuildListN<T> {
        if (current == Token.End) {
            return toRes(this, true)
        } else {
            add(fn())
            when (current) {
                Token.Comma -> advance()
                Token.End -> return toRes(this, false)
                else -> err("Missing comma")
            }
        }
    }
}

class ParenthesizedParser(filePath: URL, stream: List<PToken>, errors: MutableList<PalmError>) :
    Parser(filePath, stream, errors) {
    override val lastNewline get() = false
}

class ScopedParser(filePath: URL, stream: List<PToken>, errors: MutableList<PalmError>) :
    Parser(filePath, stream, errors) {
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

fun Parser.parseIdent(): PString {
    val curr = current
    return if (curr is Token.Ident) {
        curr.name.end()
    } else {
        err("Missing identifier")
        "".noPos()
    }
}