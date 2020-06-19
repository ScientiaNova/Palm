package com.scientianova.palm.tokenizer

import com.scientianova.palm.util.PString
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun handleIdentifier(state: ParseState) = handleIdentifier(state.code, state.pos, state.pos, StringBuilder())

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

fun handleSymbol(state: ParseState) = handleSymbol(state.code, state.pos, state.pos, StringBuilder())

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