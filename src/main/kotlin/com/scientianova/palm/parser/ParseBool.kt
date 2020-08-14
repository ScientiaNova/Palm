package com.scientianova.palm.parser

typealias ParseBool = ParseResult<Unit>

inline fun <T> ParseBool.flatMap(fn: (ParseState) -> ParseResult<T>): ParseResult<T> = when (this) {
    is ParseResult.Success -> fn(next)
    is ParseResult.Failure -> this
}

inline fun <T> ParseBool.flatMapActual(fn: (ParseState) -> ParseResult<T>): ParseResult<T> = when (this) {
    is ParseResult.Success -> fn(next.actual)
    is ParseResult.Failure -> this
}

fun parseTrue(state: ParseState) = ParseResult.Success(Unit, state)