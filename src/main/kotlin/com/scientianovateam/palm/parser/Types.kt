package com.scientianovateam.palm.parser

import com.scientianovateam.palm.registry.TypeRegistry
import com.scientianovateam.palm.tokenizer.*
import com.scientianovateam.palm.util.Positioned
import com.scientianovateam.palm.util.on
import com.scientianovateam.palm.util.safePop

data class PalmType(val clazz: Class<*>, val nullable: Boolean = false) : IOperationPart {
    fun withNullability(being: Boolean = true) = if (this.nullable == being) this else PalmType(clazz, !nullable)
}

val NULL_TYPE = PalmType(Nothing::class.java, true)

typealias PositionedType = Positioned<PalmType>

fun handleType(
    stack: TokenStack,
    token: PositionedToken?,
    startRow: Int,
    path: String = ""
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is CapitalizedIdentifierToken) {
    val next = stack.safePop()
    if (next?.value is DotToken) handleType(stack, stack.safePop(), startRow, "$path.${token.value.name}")
    else handleNullableType(
        stack, next, PalmType(
            TypeRegistry.classFromName(token.value.name, path) ?: error("Unknown type: $path.${token.value.name}")
        ) on startRow..token.rows.last
    )
} else error("Expected capitalized identifier, but instead got ${token?.javaClass}")

fun handleNullableType(
    stack: TokenStack,
    token: PositionedToken?,
    type: PositionedType
): Pair<PositionedType, PositionedToken?> =
    if (token?.value is QuestionMarkToken)
        handleNullableType(stack, stack.safePop(), type.value.withNullability(true) on type.rows.first..token.rows.last)
    else type to token

fun handleTypeString(string: String): PalmType {
    val parts = string.split(':', '.').map(String::capitalize)
    val name = parts.last()
    val path = parts.dropLast(1).joinToString(".") { it }
    return PalmType(TypeRegistry.classFromName(name, path) ?: error("Unknown type: $path.$name"))
}