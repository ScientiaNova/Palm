package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class DecPattern
typealias PDecPattern = Positioned<DecPattern>

object DecWildcardPattern : DecPattern()
data class DecNamePattern(val name: String) : DecPattern()
data class DecTuplePattern(val values: List<PDecPattern>) : DecPattern()

fun handleDeclarationPattern(
    token: PToken?,
    parser: Parser,
    error: PalmError
): Pair<PDecPattern, PToken?> = when (val value = token?.value) {
    is WildcardToken -> DecWildcardPattern at token.area to parser.pop()
    is IdentifierToken -> DecNamePattern(value.name) at token.area to parser.pop()
    is OpenParenToken -> handleTupleDecPattern(parser.pop(), parser, error, token.area.first, emptyList())
    else -> parser.error(error, token?.area ?: parser.lastArea)
}

tailrec fun handleTupleDecPattern(
    token: PToken?,
    parser: Parser,
    error: PalmError,
    startPos: StringPos,
    patterns: List<PDecPattern>
): Pair<PDecPattern, PToken?> = if (token?.value is ClosedParenToken) when (patterns.size) {
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

fun exprToDecPattern(expr: PExpression, parser: Parser, error: PalmError): PDecPattern = when (val value = expr.value) {
    is WildcardExpr -> DecWildcardPattern at expr.area
    is PathExpr ->
        if (value.parts.size == 1) DecNamePattern(value.parts.first().value) at expr.area
        else parser.error(error, expr.area)
    is TupleExpr -> DecTuplePattern(value.components.map { exprToDecPattern(it, parser, error) }) at expr.area
    else -> parser.error(error, expr.area)
}

fun getNamesInPattern(pattern: DecPattern): List<String> = when (pattern) {
    is DecNamePattern -> listOf(pattern.name)
    is DecWildcardPattern -> emptyList()
    is DecTuplePattern -> pattern.values.flatMap { getNamesInPattern(it.value) }
}