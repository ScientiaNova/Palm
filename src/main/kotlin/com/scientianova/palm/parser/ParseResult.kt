package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UncaughtParserException
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

sealed class ParseResult<out T> {
    data class Success<T>(val value: T, val next: ParseState) : ParseResult<T>()
    data class Failure(val error: PalmError, val area: StringArea) : ParseResult<Nothing>()
}

inline fun <T, S> ParseResult<T>.map(fn: (T) -> S) =
    if (this is ParseResult.Success) ParseResult.Success(fn(value), next)
    else this

inline fun <T> ParseResult<T>.flatMap(fn: (T, ParseState) -> ParseResult<T>) =
    if (this is ParseResult.Success) fn(value, next)
    else this

inline fun <T> ParseResult<T>.biFlatMap(onSucc: (T, ParseState) -> Pair<T, ParseState>, onFail: () -> Pair<T, ParseState>) = when (this) {
    is ParseResult.Success -> onSucc(value, next)
    is ParseResult.Failure -> onFail()
}

fun <T> ParseResult<T>.pairOrThrow() = when (this) {
    is ParseResult.Success -> value to next
    is ParseResult.Failure -> throw UncaughtParserException(error, area)
}

infix fun <T> T.succTo(next: ParseState) = ParseResult.Success(this, next)
infix fun <T> PalmError.errAt(area: StringArea): ParseResult<T> = ParseResult.Failure(this, area)
infix fun <T> PalmError.errAt(pos: StringPos): ParseResult<T> = ParseResult.Failure(this, pos..pos)