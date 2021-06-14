package com.palmlang.palm.lexer

import com.palmlang.palm.errors.PalmError
import com.palmlang.palm.util.StringPos
import java.net.URL

data class Lexer(
    val code: String,
    val filePath: URL,
    var pos: StringPos = 0,
    val tokens: MutableList<PToken> = mutableListOf(),
    val errors: MutableList<PalmError>,
) {
    fun Token.end(next: StringPos = pos + 1) = PToken(this, pos, next)

    fun Token.add(next: StringPos = pos + 1) = this@Lexer.also {
        pos = next
        tokens.add(PToken(this, pos, next))
    }

    fun PToken.add() = this@Lexer.also {
        pos = next
        tokens.add(this)
    }

    fun err(error: String, start: StringPos, next: StringPos = start + 1) = also {
        pos = next
        errors.add(PalmError(error, filePath, start, next))
    }

    fun err(error: PalmError) = apply {
        errors.add(error)
    }

    fun addErr(error: String, start: StringPos, next: StringPos = start + 1) = also {
        pos = next
        tokens.add(PToken(Token.Error(error), pos, next))
        errors.add(PalmError(error, filePath, start, next))
    }

    fun createErr(error: String, start: StringPos, next: StringPos = start + 1) = run {
        pos = next
        errors.add(PalmError(error, filePath, start, next))
        PToken(Token.Error(error), pos, next)
    }

    fun end(): Lexer = Token.End.add()

    fun nestedLexerAt(pos: StringPos) = Lexer(code, filePath, pos, mutableListOf(), errors)

    infix fun <T> T.succTo(next: Int): LexResult<T> = LexResult.Success(this, next)

    fun <T> String.errAt(first: StringPos, next: StringPos = first + 1): LexResult<T> =
        LexResult.Error(PalmError(this, filePath, first, next))
}