package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.alsoAdd

data class Lexer(
    val pos: StringPos = 0,
    val tokens: MutableList<PToken> = mutableListOf(),
    val errors: MutableList<PalmError> = mutableListOf()
) {
    fun Token.add(next: StringPos = pos + 1) = Lexer(next, tokens.alsoAdd(PToken(this, pos, next)), errors)
    fun err(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(next, tokens, errors.alsoAdd(PalmError(error, start, next)))

    fun err(error: PalmError) =
        Lexer(pos, tokens, errors.alsoAdd(error))

    fun addErr(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(next, tokens.alsoAdd(PToken(Token.Error(error), pos, next)), errors.alsoAdd(PalmError(error, start, next)))

    fun end(): Lexer = Token.End.add()

    fun endHere(): Lexer = Token.End.add(pos)
}