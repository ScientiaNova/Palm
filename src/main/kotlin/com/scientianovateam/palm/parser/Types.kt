package com.scientianovateam.palm.parser

import com.scientianovateam.palm.evaluator.palmType
import com.scientianovateam.palm.registry.TypeRegistry
import com.scientianovateam.palm.tokenizer.DotToken
import com.scientianovateam.palm.tokenizer.IdentifierToken
import com.scientianovateam.palm.tokenizer.PositionedToken
import com.scientianovateam.palm.tokenizer.TokenStack
import com.scientianovateam.palm.util.Positioned
import com.scientianovateam.palm.util.on
import com.scientianovateam.palm.util.safePop

data class PalmType(val clazz: Class<*>) : IOperationPart

typealias PositionedType = Positioned<PalmType>

fun handleType(
    stack: TokenStack,
    token: PositionedToken?
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = stack.safePop()
    if (next?.value is DotToken) handleType(stack, stack.safePop(), token.rows.first, token.value.name)
    else PalmType(
        TypeRegistry.classFromName(token.value.name) ?: error("Unknown type: ${token.value.name}")
    ) on token.rows.first..token.rows.last to next
} else error("Expected capitalized identifier, but instead got ${token.palmType}")

fun handleType(
    stack: TokenStack,
    token: PositionedToken?,
    startRow: Int,
    path: String
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is IdentifierToken) {
    val next = stack.safePop()
    if (next?.value is DotToken) handleType(stack, stack.safePop(), startRow, "$path.${token.value.name}")
    else PalmType(
        TypeRegistry.classFromName(token.value.name, path) ?: error("Unknown type: $path.${token.value.name}")
    ) on startRow..token.rows.last to next
} else error("Expected capitalized identifier, but instead got ${token.palmType}")

fun handleTypeString(string: String): Class<*> {
    val parts = string.split(':', '.').map(String::capitalize)
    val name = parts.last()
    val path = parts.dropLast(1).joinToString(".") { it }
    return TypeRegistry.classFromName(name, path) ?: error("Unknown type: $path.$name")
}