package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidBacktickedIdentifier
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun handleIdentifier(state: ParseState) =
    handleIdentifier(state.code, state.pos, state.pos, StringBuilder())

tailrec fun handleIdentifier(
    code: String,
    pos: StringPos,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PString, ParseState> {
    val char = code.getOrNull(pos)
    return if (char?.isIdentifierPart() == true)
        handleIdentifier(code, pos + 1, startPos, builder.append(char))
    else builder.toString() at (startPos until pos) to ParseState(code, pos)
}

fun handleBacktickedIdentifier(state: ParseState) =
    handleBacktickedIdentifier(state.code, state.pos, state.pos, StringBuilder())

tailrec fun handleBacktickedIdentifier(
    code: String,
    pos: StringPos,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PString, ParseState> = when (val char = code.getOrNull(pos)) {
    '/', '\\', '.', ';', ':', '<', '>', '[', ']', null ->
        invalidBacktickedIdentifier throwAt pos
    '`' -> builder.toString() at (startPos until pos) to ParseState(code, pos)
    else -> handleBacktickedIdentifier(code, pos + 1, startPos, builder.append(char))
}

fun handleSymbol(state: ParseState) =
    handleSymbol(state.code, state.pos, state.pos, StringBuilder())

tailrec fun handleSymbol(
    code: String,
    pos: StringPos,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PString, ParseState> {
    val char = code.getOrNull(pos)
    return if (char?.isSymbolPart() == true)
        handleSymbol(code, pos + 1, startPos, builder.append(char))
    else builder.toString() at (startPos until pos) to ParseState(code, pos)
}

val keywords = listOf("val", "var", "if", "else", "continue", "break", "for", "return", "true", "false", "null", "this", "super")