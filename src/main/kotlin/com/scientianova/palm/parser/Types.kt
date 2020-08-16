package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidTypePathError
import com.scientianova.palm.errors.missingTypeReturnTypeError
import com.scientianova.palm.errors.unclosedParenthesisError
import com.scientianova.palm.errors.unclosedSquareBacketError
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class Type
typealias PType = Positioned<Type>

typealias Path = List<PString>

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

fun handleType(state: ParseState, scoped: Boolean): ParseResult<PType> = when (state.char) {
    in identStartChars -> {
        val (ident, afterIdent) = handleIdent(state)
        handlePath(afterIdent, listOf(ident)).flatMap { path, afterPath ->
            handleWholeGenerics(afterPath, path at state.pos..afterPath.lastPos, scoped)
        }
    }
    '(' -> handleParenthesizedType(state.nextActual, state.pos, emptyList(), scoped)
    else -> unclosedSquareBacketError errAt state.pos
}

tailrec fun handlePath(state: ParseState, path: List<PString>): ParseResult<Path> {
    val actual = state.actual
    return if (actual.startWithSymbol(".")) {
        val (ident, afterIdent) = handleIdent(actual.nextActual)
        if (ident.value.isEmpty()) invalidTypePathError errAt actual
        else handlePath(afterIdent, path + ident)
    } else path succTo state
}


fun handleWholeGenerics(
    state: ParseState,
    path: Positioned<Path>,
    scoped: Boolean
): ParseResult<PType> {
    val actual = if (scoped) state.actualOrBreak else state.actual
    return if (actual.char == '[') handleGenerics(state.nextActual, emptyList()).flatMap { generics, afterGenerics ->
        NamedType(path.value, generics) at path.area.first..afterGenerics.lastPos succTo afterGenerics
    } else NamedType(path.value, emptyList()) at path.area.first..state.lastPos succTo state
}

private fun handleGenerics(
    state: ParseState,
    types: List<PType> = emptyList()
): ParseResult<List<PType>> = if (state.char == ']') types succTo state.next
else handleType(state, false).flatMap { type, afterState ->
    val symbolState = afterState.actual
    when (symbolState.char) {
        ']' -> types + type succTo symbolState.next
        ',' -> handleGenerics(symbolState.nextActual, types + type)
        else -> unclosedSquareBacketError errAt symbolState.pos
    }
}

fun handleParenthesizedType(
    state: ParseState,
    start: StringPos,
    types: List<PType>,
    scoped: Boolean
): ParseResult<PType> = if (state.char == ')') handleFunction(state.next, start, types, scoped)
else handleType(state, false).flatMap { type, afterState ->
    val actual = afterState.actual
    when (actual.char) {
        ',' -> handleParenthesizedType(actual.nextActual, start, types + type, scoped)
        ')' -> handleFunction(actual.next, start, types + type, scoped)
        else -> unclosedParenthesisError errAt actual.pos
    }
}

private fun handleFunction(state: ParseState, start: StringPos, types: List<PType>, scoped: Boolean): ParseResult<PType> {
    val (symbol, afterSymbol) = handleSymbol(state.actual)
    return when (symbol.value) {
        "->" -> handleType(afterSymbol.actual, scoped).flatMap { returnType, nextState ->
            FunctionType(types, returnType) at start..nextState.lastPos succTo nextState
        }
        "=>" -> handleType(afterSymbol.actual, scoped).flatMap { returnType, nextState ->
            ImplicitFunctionType(types, returnType) at start..nextState.lastPos succTo nextState
        }
        else -> if (types.size == 1) types.first() succTo state else missingTypeReturnTypeError errAt state
    }
}