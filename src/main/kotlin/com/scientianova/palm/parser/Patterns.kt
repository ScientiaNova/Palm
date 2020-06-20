package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map

sealed class DecPattern
typealias PDecPattern = Positioned<DecPattern>

object DecWildcardPattern : DecPattern()
data class DecNamePattern(val name: String) : DecPattern()
data class DecTuplePattern(val values: List<PDecPattern>) : DecPattern()

fun handleDeclarationPattern(
    state: ParseState,
    error: PalmError
): Pair<PDecPattern, ParseState> {
    val char = state.char
    return when {
        char == '(' -> handleTupleDecPattern(state.nextActual, error, state.pos, emptyList())
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdentifier(state)
            (if (ident.value == "_") DecWildcardPattern at state.pos
            else ident.map(::DecNamePattern)) to afterIdent
        }
        else -> error throwAt state.pos
    }
}

tailrec fun handleTupleDecPattern(
    state: ParseState,
    error: PalmError,
    startPos: StringPos,
    patterns: List<PDecPattern>
): Pair<PDecPattern, ParseState> = if (state.char == ')') when (patterns.size) {
    0 -> DecWildcardPattern at startPos..state.pos
    1 -> patterns.first()
    else -> DecTuplePattern(patterns) at startPos..state.pos
} to state.next else {
    val (pattern, afterState) = handleDeclarationPattern(state, error)
    val symbolState = afterState.actual
    when (symbolState.char) {
        ')' ->
            (if (patterns.isEmpty()) patterns.first()
            else DecTuplePattern(patterns + pattern) at startPos..symbolState.pos) to symbolState.next
        ',' -> handleTupleDecPattern(symbolState.nextActual, error, startPos, patterns + pattern)
        else -> UNCLOSED_PARENTHESIS_ERROR throwAt symbolState.pos
    }
}

fun exprToDecPattern(expr: PExpr, error: PalmError): PDecPattern = when (val value = expr.value) {
    is WildcardExpr -> DecWildcardPattern at expr.area
    is PathExpr ->
        if (value.parts.size == 1) DecNamePattern(value.parts.first().value) at expr.area
        else error throwAt expr.area
    is TupleExpr -> DecTuplePattern(value.components.map { exprToDecPattern(it, error) }) at expr.area
    else -> error throwAt expr.area
}

fun getNamesInPattern(pattern: DecPattern): List<String> = when (pattern) {
    is DecNamePattern -> listOf(pattern.name)
    is DecWildcardPattern -> emptyList()
    is DecTuplePattern -> pattern.values.flatMap { getNamesInPattern(it.value) }
}