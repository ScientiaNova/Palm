package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidBacktickedIdentifier
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun handleIdent(state: ParseState) = handleIdent(state.code, state.pos, state.pos, StringBuilder())

tailrec fun handleIdent(
    code: String,
    pos: StringPos,
    startPos: StringPos,
    builder: StringBuilder
): Pair<PString, ParseState> {
    val char = code.getOrNull(pos)
    return if (char?.isIdentifierPart() == true)
        handleIdent(code, pos + 1, startPos, builder.append(char))
    else builder.toString() at (startPos until pos) to ParseState(code, pos)
}

fun handleBacktickedIdent(state: ParseState) =
    handleBacktickedIdent(state.code, state.pos, state.pos, StringBuilder())

tailrec fun handleBacktickedIdent(
    code: String,
    pos: StringPos,
    startPos: StringPos,
    builder: StringBuilder
): ParseResult<PString> = when (val char = code.getOrNull(pos)) {
    '/', '\\', '.', ';', ':', '<', '>', '[', ']', null ->
        invalidBacktickedIdentifier failAt pos
    '`' -> builder.toString() at (startPos until pos) succTo ParseState(code, pos)
    else -> handleBacktickedIdent(code, pos + 1, startPos, builder.append(char))
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

val keywords =
    listOf("val", "var", "if", "else", "continue", "break", "for", "return", "true", "false", "null", "this", "super")