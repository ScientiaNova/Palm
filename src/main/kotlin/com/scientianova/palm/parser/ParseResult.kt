package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.unexpectedSymbolError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

sealed class ParseResult<out T> {
    data class Success<T>(val value: T, val next: ParseState) : ParseResult<T>()
    data class Failure(val error: PalmError, val area: StringArea) : ParseResult<Nothing>()
}

infix fun <T> T.succTo(next: ParseState) = ParseResult.Success(this, next)
infix fun PalmError.failAt(area: StringArea): ParseResult<Nothing> = ParseResult.Failure(this, area)
infix fun PalmError.failAt(pos: StringPos): ParseResult<Nothing> = ParseResult.Failure(this, pos..pos)
infix fun PalmError.failAt(state: ParseState): ParseResult<Nothing> = failAt(state.pos)

inline fun <T> ParseState.requireChar(char: Char, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (this.char == char) then(next) else error failAt pos

inline fun <T> ParseState.requireIdent(ident: String, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (this.startWithIdent(ident)) then(next + ident.length) else error failAt pos

inline fun <T> ParseResult<Positioned<T>>.faiLif(predicate: (T) -> Boolean, errorFn: (T) -> PalmError) =
    if (this is ParseResult.Success && predicate(value.value)) {
        ParseResult.Failure(errorFn(value.value), value.area)
    } else this

inline fun <T, S> ParseResult<T>.map(fn: (T) -> S): ParseResult<S> = when (this) {
    is ParseResult.Success -> ParseResult.Success(fn(value), next)
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.flatMap(fn: (T, ParseState) -> ParseResult<S>): ParseResult<S> = when (this) {
    is ParseResult.Success -> fn(value, next)
    is ParseResult.Failure -> this
}

fun <T, S : T> ParseResult<S>.orDefault(state: ParseState, value: T) = when (this) {
    is ParseResult.Success -> this.value to next
    is ParseResult.Failure -> value to state
}

inline fun <T, S> ParseResult<T>.flatMapIfActual(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = when (this) {
    is ParseResult.Success -> {
        val actual = next.actual
        if (predicate(actual.char)) fn(value, actual.next)
        else error failAt actual
    }
    is ParseResult.Failure -> this
}

inline fun <T, S> ParseResult<T>.flatMapIfActual(
    char: Char,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = flatMapIfActual({ it == char }, error, fn)

inline fun <T, S> ParseResult<T>.flatMapIfActualSymbol(
    symbol: String,
    error: (String) -> PalmError = ::unexpectedSymbolError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = when (this) {
    is ParseResult.Success -> {
        val (pSymbol, afterSymbol) = handleSymbol(next.actual)
        if (symbol == pSymbol.value) fn(value, afterSymbol)
        else error(symbol) failAt pSymbol.area
    }
    is ParseResult.Failure -> this
}

inline fun <T, S, C> ParseResult<T>.zipWithNext(
    zipFun: (T, S) -> C,
    nextFun: (ParseState) -> ParseResult<S>
): ParseResult<C> = when (this) {
    is ParseResult.Success -> nextFun(next).map { nextValue ->
        zipFun(value, nextValue)
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

fun <T> ParseResult<T>.expectActual(char: Char, error: PalmError) = when (this) {
    is ParseResult.Success -> {
        val actual = next.actual
        if (actual.char == char) value succTo actual.next
        else error failAt actual
    }
    is ParseResult.Failure -> this
}

inline fun <T> reuseWhileSuccess(
    startState: ParseState,
    startValue: T,
    fn: (T, ParseState) -> ParseResult<T>
): ParseResult<Nothing> {
    var state = startState
    var value = startValue
    while (true) when (val res = fn(value, state)) {
        is ParseResult.Success -> {
            state = res.next
            value = res.value
        }
        is ParseResult.Failure -> return res
    }
}

fun <T> Pair<T, ParseState>.toResult(): ParseResult<T> = ParseResult.Success(first, second)