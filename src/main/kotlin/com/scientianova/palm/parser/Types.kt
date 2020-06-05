package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_TYPE_NAME_ERROR
import com.scientianova.palm.errors.UNCLOSED_SQUARE_BRACKET_ERROR
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

sealed class Type
typealias PType = Positioned<Type>

data class NamedType(
    val path: PathExpr,
    val generics: List<PType> = emptyList()
) : Type()

data class TupleType(
    val types: List<PType> = emptyList()
) : Type()

data class FunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()

data class ImplicitFunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()

object UnitType : Type()

sealed class TypeReq
typealias PTypeReq = Positioned<TypeReq>

data class SuperTypeReq(val type: PString, val superType: PType)
data class SubTypeReq(val type: PString, val subType: PType)
data class TypeClassReq(val typeClass: PType)

fun handleType(token: PToken?, parser: Parser): Pair<PType, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> {
        val (type, next) = handleRegularType(parser.pop(), parser, listOf(value.name on token.area))
        if (next?.value == RightArrowToken) {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            FunctionType(listOf(type), returnType) on type.area.start..returnType.area.end to newNext
        } else type to next
    }
    is OpenParenToken -> handleParenthesizedType(parser.pop(), parser, token.area.start)
    else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleRegularType(token: PToken?, parser: Parser, path: List<PString>): Pair<PType, PToken?> =
    when (token?.value) {
        is DotToken -> {
            val (name, pos) = parser.pop() ?: parser.error(INVALID_TYPE_NAME_ERROR, parser.lastArea)
            handleRegularType(
                parser.pop(), parser,
                path + ((name as? IdentifierToken ?: parser.error(
                    INVALID_TYPE_NAME_ERROR,
                    parser.lastArea
                )).name on pos)
            )
        }
        is OpenSquareBracketToken -> handleGenerics(parser.pop(), parser, PathExpr(path))
        else -> NamedType(PathExpr(path)) on path.first().area.start..path.last().area.end to token
    }

tailrec fun handleGenerics(
    token: PToken?,
    parser: Parser,
    path: PathExpr,
    types: List<PType> = emptyList()
): Pair<PType, PToken?> = if (token?.value is ClosedSquareBracketToken) {
    val next = parser.pop()
    if (next?.value == OpenSquareBracketToken) handleGenerics(parser.pop(), parser, path, types)
    else NamedType(path, types) on path.parts.first().area.start..token.area.end to next
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is ClosedSquareBracketToken -> {
            val new = parser.pop()
            if (new?.value == OpenSquareBracketToken) handleGenerics(parser.pop(), parser, path, types)
            else NamedType(path, types + type) on path.parts.first().area.start..symbol.area.end to new
        }
        is CommaToken -> handleGenerics(parser.pop(), parser, path, types + type)
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleParenthesizedType(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    types: List<PType> = emptyList()
): Pair<PType, PToken?> = if (token?.value is ClosedParenToken) {
    val next = parser.pop()
    when (next?.value) {
        is RightArrowToken -> {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            FunctionType(
                if (types.isEmpty()) listOf(UnitType on start..token.area.end) else types,
                returnType
            ) on start..returnType.area.end to newNext
        }
        is ThickArrowToken -> {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            ImplicitFunctionType(
                if (types.isEmpty()) listOf(UnitType on start..token.area.end) else types,
                returnType
            ) on start..returnType.area.end to newNext
        }
        else -> when (types.size) {
            0 -> UnitType on start..token.area.end
            1 -> types.first()
            else -> TupleType(types) on start..token.area.end
        } to parser.pop()
    }
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is CommaToken -> handleParenthesizedType(parser.pop(), parser, start, types + type)
        is ClosedParenToken -> {
            val next = parser.pop()
            when (next?.value) {
                is RightArrowToken -> {
                    val (returnType, newNext) = handleType(parser.pop(), parser)
                    FunctionType(
                        if (types.isEmpty()) listOf(UnitType on symbol.area.end) else types,
                        returnType
                    ) on start..returnType.area.end to newNext
                }
                is ThickArrowToken -> {
                    val (returnType, newNext) = handleType(parser.pop(), parser)
                    ImplicitFunctionType(
                        if (types.isEmpty()) listOf(UnitType on symbol.area.end) else types,
                        returnType
                    ) on start..returnType.area.end to newNext
                }
                else -> (if (types.isEmpty()) type else TupleType(types + type) on start..symbol.area.end) to parser.pop()
            }
        }
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}