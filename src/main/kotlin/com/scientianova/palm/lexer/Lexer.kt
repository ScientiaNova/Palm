package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.ListBuilder
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.plus

data class Lexer(
    val pos: StringPos = 0,
    val tokens: ListBuilder<PToken> = ListBuilder.Null,
    val errors: ListBuilder<PalmError> = ListBuilder.Null
) {
    fun PToken.add() = Lexer(this.next, tokens + this, errors)
    fun Token.add(next: StringPos) = Lexer(next, tokens + this.till(next), errors)
    fun err(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(next, tokens, errors + PalmError(error, start, next))

    fun err(error: PalmError) =
        Lexer(pos, tokens, errors + error)

    fun addErr(error: String, start: StringPos, next: StringPos = start + 1) =
        Lexer(next, tokens + Token.Error(error).till(next), errors + PalmError(error, start, next))

    fun syncErrors(other: Lexer) = Lexer(pos, tokens, ListBuilder.ConsBranch(other.errors, errors))
}