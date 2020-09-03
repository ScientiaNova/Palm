package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringPos

sealed class LexResult<out T> {
    data class Success<T>(val value: T, val next: StringPos) : LexResult<T>()
    data class Error(val error: PalmError, val start: StringPos, val next: StringPos) : LexResult<Nothing>()
}

fun LexResult<Token>.tokenData(startPos: StringPos) = when (this) {
    is LexResult.Success -> value.data(startPos, next)
    is LexResult.Error -> error.token(startPos, next)
}

infix fun <T> T.succTo(next: Int): LexResult<T> = LexResult.Success(this, next)
fun <T> PalmError.errAt(first: StringPos, next: StringPos = first + 1): LexResult<T> = LexResult.Error(this, first, next)