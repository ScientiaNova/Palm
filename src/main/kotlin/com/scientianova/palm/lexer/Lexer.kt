package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.alsoAdd
import java.net.URL

data class Lexer(
    val code: String,
    val filePath: URL,
    val pos: StringPos = 0,
    val tokens: MutableList<PToken> = mutableListOf(),
    val errors: MutableList<PalmError>,
) {
    fun Token.add(next: StringPos = pos + 1) =
        Lexer(code, filePath, next, tokens.alsoAdd(PToken(this, pos, next)), errors)

    fun err(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(code, filePath, next, tokens, errors.alsoAdd(PalmError(error, filePath, start, next)))

    fun err(error: PalmError) =
        Lexer(code, filePath, pos, tokens, errors.alsoAdd(error))

    fun addErr(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(code, filePath, next, tokens.alsoAdd(PToken(Token.Error(error), pos, next)), errors.alsoAdd(PalmError(error, filePath, start, next)))

    fun end(): Lexer = Token.End.add()

    fun endHere(): Lexer = Token.End.add(pos)

    fun nestedLexerAt(pos: StringPos) = Lexer(code, filePath, pos, mutableListOf(), errors)

    infix fun <T> T.succTo(next: Int): LexResult<T> = LexResult.Success(this, next)

    fun <T> String.errAt(first: StringPos, next: StringPos = first + 1): LexResult<T> =
        LexResult.Error(PalmError(this, filePath, first, next))
}