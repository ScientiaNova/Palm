package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringPos

sealed class LexResult<out T> {
    data class Success<T>(val value: T, val next: StringPos) : LexResult<T>()
    data class Error(val error: PalmError) : LexResult<Nothing>()
}

infix fun <T> T.succTo(next: Int): LexResult<T> = LexResult.Success(this, next)
fun <T> String.errAt(first: StringPos, next: StringPos = first + 1): LexResult<T> =
    LexResult.Error(PalmError(this, first, next))