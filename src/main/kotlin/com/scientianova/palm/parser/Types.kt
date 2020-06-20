package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_TYPE_NAME_ERROR
import com.scientianova.palm.errors.UNCLOSED_SQUARE_BRACKET_ERROR
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

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

fun handleType(state: ParseState): Pair<PType, ParseState> = when (val value = token?.value) {
    is IdentifierToken -> {
        val (type, next) = handleRegularType(parser.pop(), parser, listOf(value.name at token.area))
        if (next?.value == RightArrowToken) {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            FunctionType(listOf(type), returnType) at type.area.first..returnType.area.last to newNext
        } else type to next
    }
    is OpenParenToken -> handleParenthesizedType(parser.pop(), parser, token.area.first)
    else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleRegularType(state: ParseState, path: List<PString>): Pair<PType, ParseState> =
    when (token?.value) {
        is DotToken -> {
            val (name, pos) = parser.pop() ?: parser.error(INVALID_TYPE_NAME_ERROR, parser.lastArea)
            handleRegularType(
                parser.pop(), parser,
                path + ((name as? IdentifierToken ?: parser.error(
                    INVALID_TYPE_NAME_ERROR,
                    parser.lastArea
                )).name at pos)
            )
        }
        is OpenSquareBracketToken -> handleGenerics(parser.pop(), parser, PathExpr(path))
        else -> NamedType(PathExpr(path)) at path.first().area.first..path.last().area.last to token
    }

tailrec fun handleGenerics(
    state: ParseState,
    path: PathExpr,
    types: List<PType> = emptyList()
): Pair<PType, ParseState> = if (token?.value is ClosedSquareBracketToken) {
    val next = parser.pop()
    if (next?.value == OpenSquareBracketToken) handleGenerics(parser.pop(), parser, path, types)
    else NamedType(path, types) at path.parts.first().area.first..token.area.last to next
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is ClosedSquareBracketToken -> {
            val new = parser.pop()
            if (new?.value == OpenSquareBracketToken) handleGenerics(parser.pop(), parser, path, types)
            else NamedType(path, types + type) at path.parts.first().area.first..symbol.area.last to new
        }
        is CommaToken -> handleGenerics(parser.pop(), parser, path, types + type)
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleParenthesizedType(
    state: ParseState
    start: StringPos,
    types: List<PType> = emptyList()
): Pair<PType, ParseState> = if (token?.value is ClosedParenToken) {
    val next = parser.pop()
    when (next?.value) {
        is RightArrowToken -> {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            FunctionType(
                if (types.isEmpty()) listOf(UnitType at start..token.area.last) else types,
                returnType
            ) at start..returnType.area.last to newNext
        }
        is ThickArrowToken -> {
            val (returnType, newNext) = handleType(parser.pop(), parser)
            ImplicitFunctionType(
                if (types.isEmpty()) listOf(UnitType at start..token.area.last) else types,
                returnType
            ) at start..returnType.area.last to newNext
        }
        else -> when (types.size) {
            0 -> UnitType at start..token.area.last
            1 -> types.first()
            else -> TupleType(types) at start..token.area.last
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
                        if (types.isEmpty()) listOf(UnitType at symbol.area.last) else types,
                        returnType
                    ) at start..returnType.area.last to newNext
                }
                is ThickArrowToken -> {
                    val (returnType, newNext) = handleType(parser.pop(), parser)
                    ImplicitFunctionType(
                        if (types.isEmpty()) listOf(UnitType at symbol.area.last) else types,
                        returnType
                    ) at start..returnType.area.last to newNext
                }
                else -> (if (types.isEmpty()) type else TupleType(types + type) at start..symbol.area.last) to parser.pop()
            }
        }
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}