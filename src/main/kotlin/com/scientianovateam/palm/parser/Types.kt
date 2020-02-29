package com.scientianovateam.palm.parser

import com.scientianovateam.palm.registry.TypeRegistry
import com.scientianovateam.palm.tokenizer.*
import com.scientianovateam.palm.util.Positioned
import com.scientianovateam.palm.util.on
import com.scientianovateam.palm.util.safePop

interface IType : IOperationPart {
    override fun toString(): String
}

typealias PositionedType = Positioned<IType>

data class RegularType(val clazz: Class<*>, val generics: List<IType> = emptyList()) : IType {
    constructor(clazz: Class<*>, vararg generics: IType) : this(clazz, generics.toList())
}

data class NullableType(val type: IType) : IType

fun handleType(stack: TokenStack, token: PositionedToken?): Pair<PositionedType, PositionedToken?> {
    val (type, next) = when (token?.value) {
        is CapitalizedIdentifierToken -> handleRegularType(stack, token, token.rows.first)
        is OpenSquareBracketToken -> {
            val (inner, bracketOrColon) = handleType(stack, stack.safePop())
            when (bracketOrColon?.value) {
                is ClosedSquareBracketToken -> RegularType(List::class.java, inner.value) on
                        token.rows.first..bracketOrColon.rows.last to stack.safePop()
                is ColonToken -> {
                    val (valType, next) = handleType(stack, stack.safePop())
                    if (next?.value !is ClosedSquareBracketToken) error("Unclosed dict type")
                    RegularType(Map::class.java, inner.value, valType.value) on
                            token.rows.first..next.rows.last to stack.safePop()
                }
                else -> error("Unclosed list type")
            }
        }
        is OpenParenToken -> {
            val (inner, next) = handleType(stack, stack.safePop())
            if (next?.value !is ClosedParenToken) error("Unclosed parenthesis")
            inner.value on token.rows.first..next.rows.last to stack.safePop()
        }
        else -> error("Invalid type")
    }
    return if (next?.value is QuestionMarkToken) NullableType(type.value) on type.rows.first..next.rows.last to stack.safePop()
    else type to next
}

fun handleRegularType(
    stack: TokenStack,
    token: PositionedToken?,
    startRow: Int,
    path: String = ""
): Pair<PositionedType, PositionedToken?> = if (token != null && token.value is CapitalizedIdentifierToken) {
    val next = stack.safePop()
    if (next?.value is DotToken) handleRegularType(stack, stack.safePop(), startRow, "$path.${token.value.name}")
    else RegularType(
        TypeRegistry.classFromName(token.value.name, path) ?: error("Unknown type: $path.${token.value.name}")
    ) on startRow..token.rows.last to next
} else error("Expected capitalized identifier, but instead got ${token?.javaClass}")

fun handleTypeString(string: String): RegularType {
    val parts = string.split(':', '.').map(String::capitalize)
    val name = parts.last()
    val path = parts.dropLast(1).joinToString(".") { it }
    return RegularType(TypeRegistry.classFromName(name, path) ?: error("Unknown type: $path.$name"))
}