package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.unexpectedSymbolError
import com.scientianova.palm.util.*

typealias SuccFn<R, A> = (A, ParseState) -> R
typealias ErrFn<R> = (PalmError, StringArea) -> R

typealias Parser<R, A> = (
    ParseState,
    SuccFn<R, A>,   //Success
    ErrFn<R>,       //Consumed Error
    ErrFn<R>,       //Empty Error
) -> R

fun <R, A, B> Parser<R, A>.map(fn: (A) -> B): Parser<R, B> = { state, succ, cErr, eErr ->
    val succ1 = { a: A, s: ParseState -> succ(fn(a), s) }
    this(state, succ1, cErr, eErr)
}

fun <R, A, B> Parser<R, A>.amap(funP: Parser<R, (A) -> B>): Parser<R, B> = { state, succ, cErr, eErr ->
    val succ1 = { fn: (A) -> B, s: ParseState ->
        val succ1 = { a: A, s1: ParseState -> succ(fn(a), s1) }
        this(s, succ1, cErr, eErr)
    }
    funP(state, succ1, cErr, eErr)
}

fun <R, A, B> Parser<R, A>.flatMap(fn: (A) -> Parser<R, B>): Parser<R, B> = { state, succ, cErr, eErr ->
    val succ1 = { a: A, s: ParseState -> fn(a)(s, succ, cErr, eErr) }
    this(state, succ1, cErr, eErr)
}

fun <R, A, B> Parser<R, A>.takeL(other: Parser<R, B>) = flatMap { value -> other.map { value } }

fun <R, A, B> Parser<R, A>.takeR(other: Parser<R, B>) = flatMap { other }

fun <R, A> oneOf(first: Parser<R, A>, vararg rest: Parser<R, A>) = oneOf(Cons(first, rest.toCons()))

fun <R, A> oneOf(parsers: Cons<Parser<R, A>>): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOf(state, succ, cErr, eErr, parsers)
}

private fun <R, A> handleOneOf(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Cons<Parser<R, A>>
): R {
    val next = parsers.next
    return if (next == null) {
        parsers.value(state, succ, cErr, eErr)
    } else {
        parsers.value(state, succ, cErr) { _, _ -> handleOneOf(state, succ, cErr, eErr, next) }
    }
}

fun <R, A> oneOfOrError(error: PalmError, vararg parsers: Parser<R, A>) = oneOfOrError(error, parsers.toCons())

fun <R, A> oneOfOrError(error: PalmError, parsers: Cons<Parser<R, A>>?): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOfOrError(state, succ, cErr, eErr, parsers, error)
}

private fun <R, A> handleOneOfOrError(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Cons<Parser<R, A>>?,
    error: PalmError
): R = if (parsers == null) {
    eErr(error, state.area)
} else {
    parsers.value(state, succ, cErr) { _, _ ->
        handleOneOfOrError(state, succ, cErr, eErr, parsers.next, error)
    }
}

fun <R, A> oneOfOrDefault(default: A, vararg parsers: Parser<R, A>) = oneOfOrDefault(default, parsers.toCons())

fun <R, A> oneOfOrDefault(default: A, parsers: Cons<Parser<R, A>>?): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOfOrDefault(state, succ, cErr, eErr, parsers, default)
}

private fun <R, A> handleOneOfOrDefault(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Cons<Parser<R, A>>?,
    default: A
): R = if (parsers == null) {
    succ(default, state)
} else {
    parsers.value(state, succ, cErr) { _, _ ->
        handleOneOfOrDefault(state, succ, cErr, eErr, parsers.next, default)
    }
}

fun <R, A> Parser<R, A>.orDefault(value: A): Parser<R, A> = { state, succ, cErr, _ ->
    this(state, succ, cErr) { _, _ -> succ(value, state) }
}

fun <R, A> Parser<R, A>.orNull() = orDefault(null)

fun <R, A> valueP(value: A): Parser<R, A> = { state, succ, _, _ -> succ(value, state) }

fun <R> matchChar(
    char: Char,
    error: PalmError = unexpectedSymbolError(char.toString())
): Parser<R, Char> = { state, succ, _, eErr ->
    if (state.char == char) {
        succ(char, state.next)
    } else {
        eErr(error, state.area)
    }
}

fun <R> matchString(
    string: String,
    error: PalmError = unexpectedSymbolError(string)
): Parser<R, String> = { state, succ, _, eErr ->
    if (state.startWith(string)) {
        succ(string, state + string.length)
    } else {
        eErr(error, state.area)
    }
}

fun <R, A> Parser<R, A>.withPos(): Parser<R, Positioned<A>> = { state, succ, cErr, eErr ->
    val succ1 = { a: A, s: ParseState -> succ(a at (state.pos until s.pos), s) }
    this(state, succ1, cErr, eErr)
}

fun <R, A, B, C> Parser<R, A>.zipWith(other: Parser<R, B>, fn: (A, B) -> C) =
    flatMap { a -> other.map { b -> fn(a, b) } }

fun <T> returnResult(state: ParseState, parser: Parser<ParseResult<T>, T>) =
    parser(state, ::success, ::error, ::error)

fun <T> returnResultT(state: ParseState, parser: Parser<ParseResultT<T>, T>) =
    parser(state, ::successT, ::errorT) { _, _ -> ParseResultT.Failure }