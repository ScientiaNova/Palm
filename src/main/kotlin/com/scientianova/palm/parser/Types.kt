package com.scientianova.palm.parser

import com.scientianova.palm.errors.UNCLOSED_SQUARE_BRACKET_ERROR
import com.scientianova.palm.errors.throwAt
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

fun handleType(state: ParseState): Pair<PType, ParseState> {
    val char = state.char
    return when {
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdentifier(state)
            val (path, afterPath) = handlePath(afterIdent, ident, emptyList())
            val (type, afterState) = handleWholeGenerics(afterPath, path, emptyList())
            val maybeArrowState = afterState.actual
            if (maybeArrowState.char?.isSymbolPart() == true) {
                val (symbol, afterSymbol) = handleSymbol(maybeArrowState)
                when (symbol.value) {
                    "->" -> {
                        val (returnType, newNext) = handleType(afterSymbol.actual)
                        FunctionType(
                            if (type.value is TupleType) type.value.types else listOf(type), returnType
                        ) at type.area.first..returnType.area.last to newNext
                    }
                    "=>" -> {
                        val (returnType, newNext) = handleType(afterSymbol.actual)
                        ImplicitFunctionType(
                            if (type.value is TupleType) type.value.types else listOf(type), returnType
                        ) at type.area.first..returnType.area.last to newNext
                    }
                    else -> type to afterState
                }
            } else type to afterState
        }
        char == '(' -> handleParenthesizedType(state.nextActual, state.pos)
        else -> UNCLOSED_SQUARE_BRACKET_ERROR throwAt state.pos
    }
}


tailrec fun handleWholeGenerics(
    state: ParseState,
    path: Positioned<PathExpr>,
    generics: List<PType>
): Pair<PType, ParseState> {
    val actual = state.actual
    return if (actual.char == '[') {
        val (newGenerics, next) = handleGenerics(state.nextActual, emptyList())
        handleWholeGenerics(next, path, generics + newGenerics)
    } else NamedType(path.value, generics) at path.area.first..state.lastPos to state
}

tailrec fun handleGenerics(
    state: ParseState,
    types: List<PType> = emptyList()
): Pair<List<PType>, ParseState> = if (state.char == ']') types to state.next else {
    val (type, afterState) = handleType(state)
    val symbolState = afterState.actual
    when (symbolState.char) {
        ']' -> types + type to state.next
        ',' -> handleGenerics(symbolState.nextActual, types + type)
        else -> UNCLOSED_SQUARE_BRACKET_ERROR throwAt symbolState.pos
    }
}

tailrec fun handleParenthesizedType(
    state: ParseState,
    start: StringPos,
    types: List<PType> = emptyList()
): Pair<PType, ParseState> = if (state.char == ')') {
    when (types.size) {
        0 -> UnitType at start..state.pos
        1 -> types.first()
        else -> TupleType(types) at start..state.pos
    } to state.next
} else {
    val (type, afterState) = handleType(state)
    val symbolState = afterState.actual
    when (symbolState.char) {
        ',' -> handleParenthesizedType(symbolState.nextActual, start, types + type)
        ')' -> (if (types.isEmpty()) type
        else TupleType(types + type) at start..symbolState.pos) to symbolState.next
        else -> UNCLOSED_SQUARE_BRACKET_ERROR throwAt symbolState.pos
    }
}