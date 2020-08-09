package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.errors.unexpectedSymbolError
import com.scientianova.palm.util.*

inline fun ParseState.require(symbol: String, error: (String) -> PalmError = ::unexpectedSymbolError): ParseState {
    val (pSymbol, next) = handleSymbol(this)
    return if (symbol == pSymbol.value) next else error(symbol) throwAt pSymbol.area
}

fun ParseState.skipWhitespace() = actual

fun ParseState.skipToLineEnd() = actualOrBreak

typealias CaseType<I, O> = (I) -> ((StringArea, ParseState) -> ParseResult<O>)?

fun <T> caseIdent(fn: (PString, ParseState) -> ParseResult<T>): CaseType<Char?, T> = { char ->
    if (char?.isLetter() == true) {
        { area, state ->
            val (ident, afterIdent) = handleIdentifier(
                state.code,
                state.pos,
                area.first,
                StringBuilder(char.toString())
            )
            fn(ident, afterIdent)
        }
    } else null
}

fun <T> caseSymbol(fn: (PString, ParseState) -> ParseResult<T>): CaseType<Char?, T> = { char ->
    if (char?.isSymbolPart() == true) {
        { area, state ->
            val (ident, afterIdent) = handleSymbol(
                state.code,
                state.pos,
                area.first,
                StringBuilder(char.toString())
            )
            fn(ident, afterIdent)
        }
    } else null
}

fun <I, O> case(match: I, then: (StringArea, ParseState) -> ParseResult<O>): CaseType<I, O> = { input ->
    if (match == input) then else null
}

inline fun <I, O> case(
    crossinline predicate: (I) -> Boolean,
    noinline then: (StringArea, ParseState) -> ParseResult<O>
): CaseType<I, O> = { input -> if (predicate(input)) then else null }

fun <I, O> usingCase(match: I, then: (Positioned<I>, ParseState) -> ParseResult<O>): CaseType<I, O> = { input ->
    if (match == input) { area, state -> then(input at area, state) } else null
}

inline fun <I, O> usingCase(
    crossinline predicate: (I) -> Boolean,
    noinline then: (I, StringArea, ParseState) -> ParseResult<O>
): CaseType<I, O> = { input -> if (predicate(input)) { area, state -> then(input, area, state) } else null }

fun <I, O> CaseType<I, O>.case(match: I, then: (StringArea, ParseState) -> ParseResult<O>): CaseType<I, O> = { input ->
    this(input) ?: if (match == input) then else null
}

inline fun <I, O> CaseType<I, O>.case(
    crossinline predicate: (I) -> Boolean,
    noinline then: (StringArea, ParseState) -> ParseResult<O>
): CaseType<I, O> = { input -> this(input) ?: if (predicate(input)) then else null }

fun <I, O> CaseType<I, O>.usingCase(match: I, then: (I, StringArea, ParseState) -> ParseResult<O>): CaseType<I, O> =
    { input ->
        this(input) ?: if (match == input) fun(area, state) = then(input, area, state) else null
    }

inline fun <I, O> CaseType<I, O>.usingCase(
    crossinline predicate: (I) -> Boolean,
    noinline then: (I, StringArea, ParseState) -> ParseResult<O>
): CaseType<I, O> = { input ->
    this(input) ?: if (predicate(input)) {
        val fn = { area: StringArea, state: ParseState -> then(input, area, state) }
        fn
    } else null
}

fun <I, O> CaseType<I, O>.otherwise(
    fn: (Positioned<I>, ParseState) -> ParseResult<O>
) = { input: Positioned<I>, state: ParseState ->
    this(input.value)?.invoke(input.area, state) ?: fn(input, state)
}

fun <I, O> CaseType<I, O>.otherwiseOrNull(
    fn: (Positioned<I>?, ParseState) -> ParseResult<O>
) = { input: Positioned<I>?, state: ParseState ->
    if (input == null) fn(input, state)
    else this(input.value)?.invoke(input.area, state) ?: fn(input, state)
}

fun <I, O> CaseType<I, O>.otherwise(
    fn: (I, StringArea, ParseState) -> ParseResult<O>
) = { input: I, area: StringArea, state: ParseState ->
    this(input)?.invoke(area, state) ?: fn(input, area, state)
}

fun <I, O> CaseType<I, O>.otherwiseOrNull(
    fn: (I, StringArea, ParseState) -> ParseResult<O>
) = { input: I, area: StringArea, state: ParseState ->
    if (input == null) fn(input, area, state)
    else this(input)?.invoke(area, state) ?: fn(input, area, state)
}

fun <I, O> CaseType<I, O>.otherwiseErr(
    error: PalmError
): (I, StringArea, ParseState) -> ParseResult<O> = otherwise { _, area, _ ->
    error errAt area
}

fun <I, O> CaseType<I, O>.otherwiseOrNullErr(
    error: PalmError
) = otherwiseOrNull { _, area, _ ->
    error errAt area
}

fun CaseType<String, PExpr>.inlineExpr(excludeCurly: Boolean) = otherwise { ident, state ->
    handleInlinedBinOps(state.actual, ident.map(::IdentExpr), excludeCurly)
}