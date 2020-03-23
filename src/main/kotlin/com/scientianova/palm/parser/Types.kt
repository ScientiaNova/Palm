package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_TYPE_NAME
import com.scientianova.palm.tokenizer.DotToken
import com.scientianova.palm.tokenizer.IdentifierToken
import com.scientianova.palm.tokenizer.PositionedToken
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

data class PalmType(val path: List<String>) : IOperationPart

typealias PositionedType = Positioned<PalmType>

fun handleType(
    parser: Parser,
    token: PositionedToken?
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = parser.pop()
    if (next?.value is DotToken) handleType(parser, parser.pop(), token.area.start, listOf(token.value.name))
    else PalmType(listOf(token.value.name)) on token.area.start..token.area.end to next
} else parser.error(INVALID_TYPE_NAME, token?.area ?: parser.lastArea)

fun handleType(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    path: List<String>
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = parser.pop()
    if (next?.value is DotToken) handleType(parser, parser.pop(), startPos, path + token.value.name)
    else PalmType(path + token.value.name) on startPos..token.area.end to next
} else parser.error(INVALID_TYPE_NAME, token?.area ?: parser.lastArea)