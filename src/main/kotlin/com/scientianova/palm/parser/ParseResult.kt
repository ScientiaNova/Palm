package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.unexpectedSymbolError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

sealed class ParseResult<out T> {
    data class Success<T>(val value: T, val next: ParseState) : ParseResult<T>()
    data class Error(val error: PalmError, val area: StringArea) : ParseResult<Nothing>()
}

sealed class ParseResultT<out T> {
    data class Success<T>(val value: T, val next: ParseState) : ParseResultT<T>()
    data class Error(val error: PalmError, val area: StringArea) : ParseResultT<Nothing>()
    object Failure : ParseResultT<Nothing>()
}

fun <T> success(value: T, next: ParseState) = ParseResult.Success(value, next)
fun error(error: PalmError, area: StringArea) = ParseResult.Error(error, area)

fun <T> successT(value: T, next: ParseState) = ParseResultT.Success(value, next)
fun errorT(error: PalmError, area: StringArea) = ParseResultT.Error(error, area)

infix fun <T> T.succTo(next: ParseState) = ParseResult.Success(this, next)
infix fun PalmError.errAt(area: StringArea): ParseResult<Nothing> = ParseResult.Error(this, area)
infix fun PalmError.errAt(pos: StringPos): ParseResult<Nothing> = ParseResult.Error(this, pos..pos)
infix fun PalmError.errAt(state: ParseState): ParseResult<Nothing> = errAt(state.pos)

inline fun <T> ParseState.requireIdent(ident: String, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (this.startWithIdent(ident)) then(next + ident.length) else error errAt pos

inline fun <T> ParseResult<Positioned<T>>.errIf(predicate: (T) -> Boolean, errorFn: (T) -> PalmError) =
    if (this is ParseResult.Success && predicate(value.value)) {
        ParseResult.Error(errorFn(value.value), value.area)
    } else this

inline fun <T, S> ParseResult<T>.map(fn: (T) -> S): ParseResult<S> = when (this) {
    is ParseResult.Success -> ParseResult.Success(fn(value), next)
    is ParseResult.Error -> this
}

inline fun <T, S> ParseResult<T>.flatMap(fn: (T, ParseState) -> ParseResult<S>): ParseResult<S> = when (this) {
    is ParseResult.Success -> fn(value, next)
    is ParseResult.Error -> this
}

fun <T, S : T> ParseResult<S>.orDefault(state: ParseState, value: T) = when (this) {
    is ParseResult.Success -> this.value to next
    is ParseResult.Error -> value to state
}

inline fun <T, S> ParseResult<T>.flatMapIfActual(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    fn: (T, ParseState) -> ParseResult<S>
): ParseResult<S> = when (this) {
    is ParseResult.Success -> {
        val actual = next.actual
        if (predicate(actual.char)) fn(value, actual.next)
        else error errAt actual
    }
    is ParseResult.Error -> this
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
        else error(symbol) errAt pSymbol.area
    }
    is ParseResult.Error -> this
}

inline fun <T, S, C> ParseResult<T>.zipWithNext(
    zipFun: (T, S) -> C,
    nextFun: (ParseState) -> ParseResult<S>
): ParseResult<C> = when (this) {
    is ParseResult.Success -> nextFun(next).map { nextValue ->
        zipFun(value, nextValue)
    }
    is ParseResult.Error -> this
}

inline fun <T, S> ParseResult<T>.biFlatMap(
    onSucc: (T, ParseState) -> ParseResult<S>,
    onFail: () -> ParseResult<S>
) = when (this) {
    is ParseResult.Success -> onSucc(value, next)
    is ParseResult.Error -> onFail()
}

fun <T> ParseResult<T>.expectActual(char: Char, error: PalmError) = when (this) {
    is ParseResult.Success -> {
        val actual = next.actual
        if (actual.char == char) value succTo actual.next
        else error errAt actual
    }
    is ParseResult.Error -> this
}

inline fun <T> loopValue(
    startValue: T,
    fn: (T) -> T
): Nothing {
    var value = startValue
    while (true) {
        value = fn(value)
    }
}