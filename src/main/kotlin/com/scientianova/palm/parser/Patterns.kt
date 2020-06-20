package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class DecPattern
typealias PDecPattern = Positioned<DecPattern>

object DecWildcardPattern : DecPattern()
data class DecNamePattern(val name: String) : DecPattern()
data class DecTuplePattern(val values: List<PDecPattern>) : DecPattern()

fun handleDeclarationPattern(
    state: ParseState,
    error: PalmError
): Pair<PDecPattern, ParseState> = when (val value = token?.value) {
    is WildcardToken -> DecWildcardPattern at token.area to parser.pop()
    is IdentifierToken -> DecNamePattern(value.name) at token.area to parser.pop()
    is OpenParenToken -> handleTupleDecPattern(parser.pop(), parser, error, token.area.first, emptyList())
    else -> parser.error(error, token?.area ?: parser.lastArea)
}

tailrec fun handleTupleDecPattern(
    state: ParseState,
    error: PalmError,
    startPos: StringPos,
    patterns: List<PDecPattern>
): Pair<PDecPattern, ParseState> = if (token?.value is ClosedParenToken) when (patterns.size) {
    0 -> DecWildcardPattern at startPos..token.area.last
    1 -> patterns.first()
    else -> DecTuplePattern(patterns) at startPos..token.area.last
} to parser.pop() else {
    val (pattern, symbol) = handleDeclarationPattern(token, parser, error)
    when (symbol?.value) {
        is ClosedParenToken ->
            (if (patterns.isEmpty()) patterns.first()
            else DecTuplePattern(patterns + pattern) at startPos..symbol.area.last) to parser.pop()
        is CommaToken -> handleTupleDecPattern(parser.pop(), parser, error, startPos, patterns + pattern)
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, startPos)
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