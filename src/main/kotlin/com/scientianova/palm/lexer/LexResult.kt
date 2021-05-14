package com.scientianova.palm.lexer

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.util.StringPos

sealed class LexResult<out T> {
    data class Success<T>(val value: T, val next: StringPos) : LexResult<T>()
    data class Error(val error: PalmError) : LexResult<Nothing>()
}