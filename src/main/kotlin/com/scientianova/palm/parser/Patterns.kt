package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

sealed class Pattern
typealias PPattern = Positioned<Pattern>

object WildcardPattern : Pattern()
object UnitPattern : Pattern()

data class TuplePattern(
    val components: List<PPattern>
) : Pattern()

data class ExpressionPattern(
    val expr: Expression
) : Pattern()

data class DeclarationPattern(
    val name: PString,
    val type: PType?,
    val mutable: Boolean
) : Pattern()

sealed class DecPattern
typealias PDecPattern = Positioned<DecPattern>

object DecWildcardPattern : DecPattern()
data class DecNamePattern(val name: String, val mutable: Boolean) : DecPattern()
data class DecTuplePattern(val values: List<PDecPattern>) : DecPattern()

fun handleDeclarationPattern(
    token: PToken?,
    parser: Parser,
    error: PalmError
): Pair<PDecPattern, PToken?> = when (val value = token?.value) {
    is WildcardToken -> DecWildcardPattern on token.area to parser.pop()
    is MutToken -> {
        val next = parser.pop()
        if (next != null && next.value is IdentifierToken)
            DecNamePattern(next.value.name, true) on next.area to parser.pop()
        else DecNamePattern("mut", false) on token.area to next
    }
    is IdentifierToken -> DecNamePattern(value.name, false) on token.area to parser.pop()
    is OpenParenToken -> handleTupleDecPattern(parser.pop(), parser, error, token.area.start, emptyList())
    else -> parser.error(error, token?.area ?: parser.lastArea)
}

tailrec fun handleTupleDecPattern(
    token: PToken?,
    parser: Parser,
    error: PalmError,
    startPos: StringPos,
    patterns: List<PDecPattern>
): Pair<PDecPattern, PToken?> = if (token?.value is ClosedParenToken) when (patterns.size) {
    0 -> DecWildcardPattern on startPos..token.area.end
    1 -> patterns.first()
    else -> DecTuplePattern(patterns) on startPos..token.area.end
} to parser.pop() else {
    val (pattern, symbol) = handleDeclarationPattern(token, parser, error)
    when (symbol?.value) {
        is ClosedParenToken ->
            (if (patterns.isEmpty()) patterns.first()
            else DecTuplePattern(patterns + pattern) on startPos..symbol.area.end) to parser.pop()
        is CommaToken -> handleTupleDecPattern(parser.pop(), parser, error, startPos, patterns + pattern)
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, startPos)
    }
}

fun exprToDecPattern(expr: PExpression, parser: Parser, error: PalmError): PDecPattern = when (val value = expr.value) {
    is WildcardExpr, is UnitExpr -> DecWildcardPattern on expr.area
    is PathExpr ->
        if (value.parts.size == 1) DecNamePattern(value.parts.first().value, false) on expr.area
        else parser.error(error, expr.area)
    is TupleExpr -> DecTuplePattern(value.components.map { exprToDecPattern(it, parser, error) }) on expr.area
    else -> parser.error(error, expr.area)
}