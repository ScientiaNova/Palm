package com.scientianova.palm.parser

import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.registry.TypeName
import com.scientianova.palm.registry.TypeRegistry
import com.scientianova.palm.tokenizer.DotToken
import com.scientianova.palm.tokenizer.IdentifierToken
import com.scientianova.palm.tokenizer.PositionedToken
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

data class PalmType(val clazz: Class<*>) : IOperationPart

typealias PositionedType = Positioned<PalmType>

fun handleType(
    parser: Parser,
    token: PositionedToken?
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = parser.pop()
    if (next?.value is DotToken) handleType(parser, parser.pop(), token.area.start, token.value.name)
    else PalmType(
        TypeRegistry.classFromName(token.value.name) ?: error("Unknown type: ${token.value.name}")
    ) on token.area.start..token.area.end to next
} else error("Was expected a type name, but instead got ${token.palmType}")

fun handleType(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    path: String
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = parser.pop()
    if (next?.value is DotToken) handleType(parser, parser.pop(), startPos, "$path.${token.value.name}")
    else PalmType(
        TypeRegistry.classFromName(token.value.name, path) ?: error("Unknown type: $path.${token.value.name}")
    ) on startPos..token.area.end to next
} else error("Was expected a type name, but instead got ${token.palmType}")

fun handleTypeString(string: String): TypeName {
    val parts = string.replace(':', '.')
    val name = parts.takeWhile { it != '.' }
    val path = parts.dropLast(name.length + 1)
    return TypeName(path, name)
}