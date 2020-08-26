@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidBacktickedIdentifier
import com.scientianova.palm.errors.missingIdentifierError
import com.scientianova.palm.errors.missingSymbolError
import com.scientianova.palm.util.StringPos

fun <R> identifier() = identifier as Parser<R, String>

private val identifier: Parser<Any, String> = oneOf(normalIdentifier(), tickedIdentifier())

fun <R> normalIdentifier() = normalIdentifier as Parser<R, String>

private val normalIdentifier: Parser<Any, String> = { state, succ, _, eErr ->
    val char = state.char
    if (char != null && char.isIdentifierStart()) {
        handleNormalIdent(state.code, state.nextPos, StringBuilder().append(char), succ)
    } else {
        eErr(missingIdentifierError, state.area)
    }
}

private tailrec fun <R> handleNormalIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, String>
): R {
    val char = code.getOrNull(pos)
    return if (char?.isIdentifierPart() == true) {
        handleNormalIdent(code, pos + 1, builder.append(char), succFn)
    } else {
        succFn(builder.toString(), ParseState(code, pos))
    }
}

fun <R> tickedIdentifier() = tickedIdentifier as Parser<R, String>

private val tickedIdentifier: Parser<Any, String> =
    matchChar<Any>('`', missingIdentifierError).takeR { state, succ, cErr, _ ->
        handleTickedIdent(state.code, state.pos, StringBuilder(), succ, cErr)
    }

private tailrec fun <R> handleTickedIdent(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, String>,
    errFn: ErrFn<R>
): R = when (val char = code.getOrNull(pos)) {
    '/', '\\', '.', ';', ':', '<', '>', '[', ']', null ->
        errFn(invalidBacktickedIdentifier, pos..pos)
    '`' -> succFn(builder.toString(), ParseState(code, pos + 1))
    else -> handleTickedIdent(code, pos + 1, builder.append(char), succFn, errFn)
}

fun <R> symbol() = symbol as Parser<R, String>

private val symbol: Parser<Any, String> = { state, succ, _, eErr ->
    val char = state.char
    if (char != null && char.isSymbolPart()) {
        handleSymbol(state.code, state.nextPos, StringBuilder().append(char), succ, eErr)
    } else {
        eErr(missingSymbolError, state.area)
    }
}

private tailrec fun <R> handleSymbol(
    code: String,
    pos: StringPos,
    builder: StringBuilder,
    succFn: SuccFn<R, String>,
    errFn: ErrFn<R>
): R {
    val char = code.getOrNull(pos)
    return if (char?.isSymbolPart() == true)
        handleSymbol(code, pos + 1, builder.append(char), succFn, errFn)
    else succFn(builder.toString(), ParseState(code, pos))
}

val keywords =
    listOf("val", "var", "if", "else", "continue", "break", "for", "return", "true", "false", "null", "this", "super")