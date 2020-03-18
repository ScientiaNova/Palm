package com.scientianova.palm.parser

import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.registry.TypeName
import com.scientianova.palm.registry.TypeRegistry
import com.scientianova.palm.tokenizer.DotToken
import com.scientianova.palm.tokenizer.IdentifierToken
import com.scientianova.palm.tokenizer.PositionedToken
import com.scientianova.palm.tokenizer.TokenList
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.on

data class PalmType(val clazz: Class<*>) : IOperationPart

typealias PositionedType = Positioned<PalmType>

fun handleType(
    list: TokenList,
    token: PositionedToken?
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = list.poll()
    if (next?.value is DotToken) handleType(list, list.poll(), token.rows.first, token.value.name)
    else PalmType(
        TypeRegistry.classFromName(token.value.name) ?: error("Unknown type: ${token.value.name}")
    ) on token.rows.first..token.rows.last to next
} else error("Expected capitalized identifier, but instead got ${token.palmType}")

fun handleType(
    list: TokenList,
    token: PositionedToken?,
    startRow: Int,
    path: String
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = list.poll()
    if (next?.value is DotToken) handleType(list, list.poll(), startRow, "$path.${token.value.name}")
    else PalmType(
        TypeRegistry.classFromName(token.value.name, path) ?: error("Unknown type: $path.${token.value.name}")
    ) on startRow..token.rows.last to next
} else error("Expected capitalized identifier, but instead got ${token.palmType}")

fun handleTypeString(string: String): TypeName {
    val parts = string.replace(':', '.')
    val name = parts.takeWhile { it != '.' }
    val path = parts.dropLast(name.length + 1)
    return TypeName(path, name)
}