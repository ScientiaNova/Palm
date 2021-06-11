package com.palmlang.palm.lexer

import com.palmlang.palm.errors.PalmError
import com.palmlang.palm.util.StringPos

sealed class LexResult<out T> {
    data class Success<T>(val value: T, val next: StringPos) : LexResult<T>()
    data class Error(val error: PalmError) : LexResult<Nothing>()
}