package com.scientianova.palm.parser

import com.scientianova.palm.errors.PError
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.notEOLError
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

fun <R, A, B> Parser<R, A>.takeRLazy(fn: () -> Parser<R, B>): Parser<R, B> {
    val lazy = lazy(fn)
    return flatMap { lazy.value }
}

fun <R, A> Parser<R, A>.or(other: Parser<R, A>): Parser<R, A> = { state, succ, cErr, eErr ->
    this(state, succ, cErr) { _, _ -> other(state, succ, cErr, eErr) }
}

fun <R, A> oneOf(vararg parsers: Parser<R, A>): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOf(state, succ, cErr, eErr, parsers, 0)
}

private fun <R, A> handleOneOf(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Array<out Parser<R, A>>,
    index: Int
): R = if (index == parsers.lastIndex) {
    parsers[index](state, succ, cErr, eErr)
} else {
    parsers[index](state, succ, cErr) { _, _ -> handleOneOf(state, succ, cErr, eErr, parsers, index + 1) }
}

fun <R, A> oneOfOrError(error: PalmError, vararg parsers: Parser<R, A>): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOfOrError(state, succ, cErr, eErr, parsers, error, 0)
}

private fun <R, A> handleOneOfOrError(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Array<out Parser<R, A>>,
    error: PalmError,
    index: Int
): R = if (index == parsers.size) {
    eErr(error, state.area)
} else {
    parsers[index](state, succ, cErr) { _, _ ->
        handleOneOfOrError(state, succ, cErr, eErr, parsers, error, index + 1)
    }
}

fun <R, A> oneOfOrDefault(default: A, vararg parsers: Parser<R, A>): Parser<R, A> = { state, succ, cErr, eErr ->
    handleOneOfOrDefault(state, succ, cErr, eErr, parsers, default, 0)
}

private fun <R, A> handleOneOfOrDefault(
    state: ParseState,
    succ: (A, ParseState) -> R,
    cErr: (PalmError, StringArea) -> R,
    eErr: (PalmError, StringArea) -> R,
    parsers: Array<out Parser<R, A>>,
    default: A,
    index: Int
): R = if (index == parsers.size) {
    succ(default, state)
} else {
    parsers[index](state, succ, cErr) { _, _ ->
        handleOneOfOrDefault(state, succ, cErr, eErr, parsers, default, index + 1)
    }
}

fun <R, A> Parser<R, A>.orDefault(value: A): Parser<R, A> = { state, succ, cErr, _ ->
    this(state, succ, cErr) { _, _ -> succ(value, state) }
}

fun <R, A> Parser<R, A>.orNull() = orDefault(null)

fun <R, A> valueP(value: A): Parser<R, A> = { state, succ, _, _ -> succ(value, state) }

fun <R, A> consumedError(error: PalmError): Parser<R, A> = { state, _, cErr, _ ->
    cErr(error, state.area)
}

fun <R, A> consumedError(error: PalmError, area: StringArea): Parser<R, A> = { state, _, cErr, _ ->
    cErr(error, area)
}

fun <R, A> emptyError(error: PalmError): Parser<R, A> = { state, _, _, eErr ->
    eErr(error, state.area)
}

fun <R> tryChar(
    char: Char,
    error: PalmError = unexpectedSymbolError(char.toString())
): Parser<R, Char> = { state, succ, _, eErr ->
    if (state.char == char) {
        succ(char, state.next)
    } else {
        eErr(error, state.area)
    }
}

fun <R> requireChar(
    char: Char,
    error: PalmError = unexpectedSymbolError(char.toString())
): Parser<R, Char> = { state, succ, cErr, _ ->
    if (state.char == char) {
        succ(char, state.next)
    } else {
        cErr(error, state.area)
    }
}

fun <R> tryString(
    string: String,
    error: PalmError = unexpectedSymbolError(string)
): Parser<R, String> = { state, succ, _, eErr ->
    if (state.startsWith(string)) {
        succ(string, state + string.length)
    } else {
        eErr(error, state.area)
    }
}

fun <R> tryIdent(
    string: String,
    error: PalmError = unexpectedSymbolError(string)
): Parser<R, String> = { state, succ, _, eErr ->
    if (state.startsWithIdent(string)) {
        succ(string, state + string.length)
    } else {
        eErr(error, state.area)
    }
}

fun <R> requireIdent(
    string: String,
    error: PalmError = unexpectedSymbolError(string)
): Parser<R, String> = { state, succ, cErr, _ ->
    if (state.startsWithIdent(string)) {
        succ(string, state + string.length)
    } else {
        cErr(error, state.area)
    }
}

fun <R> requireSymbol(
    string: String,
    error: PalmError = unexpectedSymbolError(string)
): Parser<R, String> = { state, succ, cErr, _ ->
    if (state.startsWithSymbol(string)) {
        succ(string, state + string.length)
    } else {
        cErr(error, state.area)
    }
}

fun <R, A> Parser<R, A>.withPos(): Parser<R, Positioned<A>> = { state, succ, cErr, eErr ->
    val succ1 = { a: A, s: ParseState -> succ(a at (state.pos until s.pos), s) }
    this(state, succ1, cErr, eErr)
}

fun <R, A, B, C> Parser<R, A>.zipWith(other: Parser<R, B>, fn: (A, B) -> C) =
    flatMap { a -> other.map { b -> fn(a, b) } }

fun <R, A, B, C, D> Parser<R, A>.zipWith2(second: Parser<R, B>, third: Parser<R, C>, fn: (A, B, C) -> D) =
    flatMap { a -> second.flatMap { b -> third.map { c -> fn(a, b, c) } } }

fun <T> returnResult(state: ParseState, parser: Parser<ParseResult<T>, T>) =
    parser(state, ::success, ::error, ::error)

fun <T> returnResultT(state: ParseState, parser: Parser<ParseResultT<T>, T>) =
    parser(state, ::successT, ::errorT) { _, _ -> ParseResultT.Failure }

fun <R, A> elevateError(parser: Parser<R, A>): Parser<R, A> = { state, succ, cErr, _ ->
    parser(state, succ, cErr, cErr)
}

fun <R, A> Parser<R, Positioned<A>>.failIf(
    errFn: (A) -> PalmError,
    predicate: (A) -> Boolean
): Parser<R, Positioned<A>> =
    { state, succ, cErr, eErr ->
        val succ1 =
            { a: Positioned<A>, s: ParseState -> if (predicate(a.value)) cErr(errFn(a.value), a.area) else succ(a, s) }
        this(state, succ1, cErr, eErr)
    }

fun <R, A> loopingBodyParser(
    endChar: Char,
    elemParser: Parser<ParseResult<A>, A>,
    unclosedError: PalmError
): Parser<R, List<A>> = parser@{ startState, succ, cErr, _ ->
    loopValue(emptyList<A>() to startState.actual) { (list, state) ->
        if (state.char == endChar) {
            return@parser succ(list, state.next)
        } else when (val res = returnResult(state, elemParser)) {
            is ParseResult.Success -> {
                val symbolState = res.next.actual
                when (symbolState.char) {
                    ',' -> list + res.value to symbolState.next
                    endChar -> return@parser succ(list + res.value, symbolState.next)
                    else -> return@parser cErr(unclosedError, symbolState.area)
                }
            }
            is ParseResult.Error -> return@parser cErr(res.error, res.area)
        }
    }
}

private fun <A> parseSuccess(a: A, state: ParseState) = if (state.char == null) {
    Right(a)
} else {
    Left(notEOLError at state.area)
}

private fun <A> parseError(error: PalmError, area: StringArea): Either<PError, A> = Left(error at area)

fun <A> parse(startState: ParseState, parser: Parser<Either<PError, A>, A>) =
    parser(startState, ::parseSuccess, ::parseError, ::parseError)