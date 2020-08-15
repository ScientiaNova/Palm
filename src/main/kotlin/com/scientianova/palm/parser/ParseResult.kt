package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UncaughtParserException
import com.scientianova.palm.errors.unexpectedSymbolError
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

sealed class ParseResult<out T> {
    data class Success<T>(val value: T, val next: ParseState) : ParseResult<T>()
    data class Failure(val error: PalmError, val area: StringArea) : ParseResult<Nothing>()
}

inline fun <T, S> ParseResult<T>.map(fn: (T) -> S): ParseResult<S> = when (this) {
    is ParseResult.Success -> ParseResult.Success(fn(value), next)
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.flatMap(fn: (T, ParseState) -> ParseResult<S>): ParseResult<S> = when (this) {
    is ParseResult.Success -> fn(value, next)
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.baseFlatMapIf(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    checking: (ParseState) -> ParseState,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = when (this) {
    is ParseResult.Success -> {
        if (predicate(checking(next).char)) fn(value, next.next)
        else error errAt next
    }
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.flatMapIfActual(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = baseFlatMapIf(predicate, error, ParseState::actual, fn)

inline fun <T, S> ParseResult<T>.flatMapIfActualOnLine(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = baseFlatMapIf(predicate, error, ParseState::actualOrBreak, fn)

inline fun <T, S> ParseResult<T>.baseFlatMapIf(
    char: Char,
    error: PalmError,
    checking: (ParseState) -> ParseState,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = baseFlatMapIf({ it == char }, error, checking, fn)

inline fun <T, S> ParseResult<T>.flatMapIfActual(
    char: Char,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = baseFlatMapIf(char, error, ParseState::actual, fn)

inline fun <T, S> ParseResult<T>.flatMapIfActualOnLine(
    char: Char,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = baseFlatMapIf(char, error, ParseState::actualOrBreak, fn)

inline fun <T, S> ParseResult<T>.flatMapIfActualSymbol(
    symbol: String,
    error: (String) -> PalmError = ::unexpectedSymbolError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = when (this) {
    is ParseResult.Success -> {
        val (pSymbol, afterSymbol) = handleSymbol(next.actual)
        if (symbol == pSymbol.value) fn(value, afterSymbol)
        else error(symbol) errAt pSymbol.area
    }
    is ParseResult.Failure -> this
}

inline fun <T, S, C> ParseResult<T>.zipWithNext(
    zipFun: (T, S) -> C,
    nextFun: (ParseState) -> ParseResult<S>
): ParseResult<C> = when (this) {
    is ParseResult.Success -> nextFun(next).flatMap { nextValue, nextState ->
        zipFun(value, nextValue) succTo nextState
    }
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.biFlatMap(
    onSucc: (T, ParseState) -> ParseResult<S>,
    onFail: () -> ParseResult<S>
) = when (this) {
    is ParseResult.Success -> onSucc(value, next)
    is ParseResult.Failure -> onFail()
}

fun <T> ParseResult<T>.pairOrThrow() = when (this) {
    is ParseResult.Success -> value to next
    is ParseResult.Failure -> throw UncaughtParserException(error, area)
}

infix fun <T> T.succTo(next: ParseState) = ParseResult.Success(this, next)
infix fun PalmError.errAt(area: StringArea): ParseResult<Nothing> = ParseResult.Failure(this, area)
infix fun PalmError.errAt(pos: StringPos): ParseResult<Nothing> = ParseResult.Failure(this, pos..pos)
infix fun PalmError.errAt(state: ParseState): ParseResult<Nothing> = errAt(state.pos)