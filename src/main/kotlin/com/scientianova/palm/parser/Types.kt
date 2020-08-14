package com.scientianova.palm.parser

import com.scientianova.palm.errors.emptyParenthesesOnPatternError
import com.scientianova.palm.errors.missingTypeReturnTypeError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.errors.unclosedSquareBacketError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class Type
typealias PType = Positioned<Type>

typealias Path = List<String>

data class NamedType(
    val path: Path,
    val generics: List<PType> = emptyList()
) : Type()

data class FunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()

data class ImplicitFunctionType(
    val params: List<PType>,
    val returnType: PType
) : Type()

fun handleType(state: ParseState): ParseResult<PType> {
    val char = state.char
    return when {
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdent(state)
            val (path, afterPath) = handlePath(afterIdent, ident, emptyList())
            handleWholeGenerics(afterPath, path, emptyList()).flatMap { type, afterState ->
                type succTo afterState
            }
        }
        char == '(' -> handleParenthesizedType(state.nextActual, state.pos, emptyList())
        else -> unclosedSquareBacketError errAt state.pos
    }
}


tailrec fun handleWholeGenerics(
    state: ParseState,
    path: Positioned<Path>,
    generics: List<PType>
): ParseResult<PType> {
    val actual = state.actual
    return if (actual.char == '[') handleGenerics(state.nextActual, emptyList()).flatMap { newGenerics, next ->
        return handleWholeGenerics(next, path, generics + newGenerics)
    } else NamedType(path.value, generics) at path.area.first..state.lastPos succTo state
}

tailrec fun handleGenerics(
    state: ParseState,
    types: List<PType> = emptyList()
): ParseResult<List<PType>> = if (state.char == ']') types succTo state.next
else handleType(state).flatMap { type, afterState ->
    val symbolState = afterState.actual
    when (symbolState.char) {
        ']' -> types + type succTo state.next
        ',' -> return handleGenerics(symbolState.nextActual, types + type)
        else -> unclosedSquareBacketError errAt symbolState.pos
    }
}

tailrec fun handleParenthesizedType(
    state: ParseState,
    start: StringPos,
    types: List<PType>
): ParseResult<PType> = if (state.char == ')') handleFunction(state.next, start, types)
else handleType(state).flatMap { type, afterState ->
    val actual = afterState.actual
    when (actual.char) {
        ',' -> return handleParenthesizedType(actual.nextActual, start, types + type)
        ')' -> handleFunction(actual.next,start, types + type)
        else -> unclosedParenthesisError errAt actual.pos
    }
}

private fun handleFunction(state: ParseState, start: StringPos, types: List<PType>): ParseResult<PType> {
    val (symbol, afterSymbol) = handleSymbol(state.actual)
    return when (symbol.value) {
        "->" -> handleType(afterSymbol.actual).flatMap { returnType, nextState ->
            FunctionType(types, returnType) at start..nextState.lastPos succTo nextState
        }
        "=>" -> handleType(afterSymbol.actual).flatMap { returnType, nextState ->
            ImplicitFunctionType(types, returnType) at start..nextState.lastPos succTo nextState
        }
        else -> if (types.size == 1) types.first() succTo state else missingTypeReturnTypeError errAt state
    }
}