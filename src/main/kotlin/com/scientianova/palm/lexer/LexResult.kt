package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

sealed class LexResult<out T> {
    data class Success<T>(val value: T, val next: Int) : LexResult<T>()
    data class Error(val error: PalmError, val area: StringArea) : LexResult<Nothing>()
}

infix fun <T> T.succTo(next: Int): LexResult<T> = LexResult.Success(this, next)

fun <T> PalmError.errAt(first: StringPos, last: StringPos): LexResult<T> = LexResult.Error(this, StringArea(first, last))
infix fun <T> PalmError.errAt(pos: StringPos): LexResult<T> = LexResult.Error(this, pos..pos)
infix fun <T> PalmError.errAt(area: StringArea): LexResult<T> = LexResult.Error(this, area)