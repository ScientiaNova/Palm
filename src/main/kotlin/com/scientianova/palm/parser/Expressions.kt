package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

sealed class Expression
typealias PExpr = Positioned<Expression>

data class PathExpr(
    val parts: List<PString>
) : Expression()

data class CallExpr(
    val expr: PExpr,
    val params: List<PExpr>
) : Expression()

data class LambdaExpr(
    val params: List<PDecPattern>,
    val expr: PExpr
) : Expression()

data class WhenExpr(
    val comparing: PExpr,
    val branches: List<Pair<PExpr, PExpr>>
) : Expression()

data class DecExpr(
    val pattern: PExpr,
    val value: PExpr?
) : Expression()

data class ScopeExpr(val statements: List<PStatement>) : Expression()

data class ByteExpr(val value: Byte) : Expression()
data class ShortExpr(val value: Short) : Expression()
data class IntExpr(val value: Int) : Expression()
data class LongExpr(val value: Long) : Expression()
data class FloatExpr(val value: Float) : Expression()
data class DoubleExpr(val value: Double) : Expression()
data class CharExpr(val value: Char) : Expression()

object UnitExpr : Expression()
object WildcardExpr : Expression()

data class StringExpr(val string: String) : Expression()

data class TupleExpr(
    val components: List<PExpr>
) : Expression()

data class ListExpr(
    val components: List<PExpr>,
    val mutable: Boolean
) : Expression()

data class ArrayExpr(
    val components: List<PExpr>
) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String) : BinOp()

val PLUS = SymbolOp("+")

data class PrefixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class BinaryOpsExpr(val body: List<Pair<PExpr, PBinOp>>, val end: PExpr) : Expression()

data class TypeExpr(val expr: PExpr, val type: PType) : Expression()

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleScope(
    state: ParseState,
    start: StringPos,
    returnBracket: Boolean,
    statements: List<PStatement> = emptyList()
): Pair<PExpr, ParseState> {
    val char = state.char
    return when {
        char == null -> UNCLOSED_SCOPE_ERROR throwAt state.pos
        char == '{' -> {
            if (returnBracket) ScopeExpr(statements) at start..state.pos to state
            else ScopeExpr(statements) at start..state.pos to state.next
        }
        char.isSeparator() -> handleScope(state.nextActual, start, returnBracket, statements)
        char.isLetter() -> {
            val (ident, afterIdent) = handleIdentifier(state)
            val (statement, afterState) = if (ident.value == "let") {
                handleDeclaration(afterIdent.actual, state.pos)
            } else {
                val (sub, next) = handleSubexpression(state)
                handleFirstBinOp(next.actualOrBreak, sub)
            }
            val sepState = afterState.actualOrBreak
            if (sepState.char.isExpressionSeparator())
                handleScope(sepState.nextActual, start, returnBracket, statements + statement)
            else UNCLOSED_WHEN_ERROR throwAt sepState.pos
        }
        else -> {
            val (sub, next) = handleSubexpression(state)
            val (expr, afterState) = handleFirstBinOp(next.actualOrBreak, sub)

            val sepState = afterState.actualOrBreak
            if (sepState.char.isExpressionSeparator())
                handleScope(sepState.nextActual, start, returnBracket, statements + expr)
            else UNCLOSED_WHEN_ERROR throwAt sepState.pos
        }
    }
}

fun handleInlinedExpression(
    state: ParseState,
    excludeCurly: Boolean = false,
    excludeArrow: Boolean = false
): Pair<PExpr, ParseState> {
    val (firstPart, next) = handleSubexpression(state)
    return handleInlinedBinOps(next, firstPart, excludeCurly, excludeArrow)
}

fun handleInScopeExpression(state: ParseState): Pair<PExpr, ParseState> {
    val (firstPart, next) = handleSubexpression(state)
    return handleScopedBinOps(next, firstPart)
}

tailrec fun handleInlinedBinOps(
    state: ParseState,
    last: PExpr,
    excludeCurly: Boolean,
    excludeArrow: Boolean,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): Pair<PExpr, ParseState> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (afterOp.char.isAfterPostfix())
        handleInlinedBinOps(
            afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, excludeCurly,
            excludeArrow, body
        )
    else {
        val (sub, next) = handleSubexpression(afterOp.actual)
        handleInlinedBinOps(next, sub, excludeCurly, excludeArrow, body + (last to op.map(::SymbolOp)))
    }
} else {
    val actual = state.actual
    val actualChar = actual.char
    when {
        actualChar == null ->
            (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
        actualChar.isLetter() -> {
            val (infix, afterInfix) = handleIdentifier(actual)
            val (part, next) = handleSubexpression(afterInfix.nextActual)
            handleInlinedBinOps(next, part, excludeCurly, excludeArrow, body + (last to infix.map(::InfixCall)))
        }
        actualChar.isSymbolPart() -> {
            val (op, afterOp) = handleSymbol(state)
            if (afterOp.char?.isWhitespace() == false)
                INVALID_PREFIX_OPERATOR_ERROR throwAt afterOp.pos
            val opSymbol = op.value
            when {
                opSymbol == ":" -> {
                    val (type, next) = handleType(afterOp.actual)
                    handleInlinedBinOps(
                        next, TypeExpr(last, type) at last.area.first..type.area.last,
                        excludeCurly, excludeArrow, body
                    )
                }
                !excludeArrow && opSymbol == "->" -> {
                    val input = exprToDecPattern(last, INVALID_PARAMETER_ERROR)
                    val actualInput =
                        if (input.value is DecTuplePattern) input.value.values
                        else listOf(input)
                    val (lamExpr, next) = handleInScopeExpression(afterOp.actual)
                    handleInlinedBinOps(
                        next, LambdaExpr(actualInput, lamExpr) at last.area.first..lamExpr.area.last,
                        excludeCurly, excludeArrow, body
                    )
                }
                opSymbol == "<-" -> INVALID_LEFT_ARROW_ERROR throwAt op.area
                opSymbol == "==" -> INVALID_ASSIGNMENT_ERROR throwAt op.area
                else -> {
                    val (part, next) = handleSubexpression(afterOp.actual)
                    handleInlinedBinOps(
                        next.actual, part, excludeCurly, excludeArrow, body + (last to op.map(::SymbolOp))
                    )
                }
            }
        }
        actualChar == '(' -> {
            val (params, next) = handleParams(actual.nextActual, actual.pos)
            handleInlinedBinOps(
                next, CallExpr(last, params.value) at last.area.first..params.area.last,
                excludeCurly, excludeArrow, body
            )
        }
        !excludeCurly && actualChar == '{' -> {
            val (scope, next) = handleScope(actual.nextActual, actual.pos, false)
            handleInlinedBinOps(
                next, CallExpr(last, listOf(scope)) at last.area.first..scope.area.last,
                excludeCurly, excludeArrow, body
            )
        }
        else ->
            (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
    }
}

tailrec fun handleScopedBinOps(
    state: ParseState,
    last: PExpr,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): Pair<PExpr, ParseState> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (afterOp.char.isAfterPostfix())
        handleScopedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, body)
    else {
        val (sub, next) = handleSubexpression(afterOp.actual)
        handleScopedBinOps(next, sub, body + (last to op.map(::SymbolOp)))
    }
} else {
    val actualOnLine = state.actualOrBreak
    val actualChar = actualOnLine.char
    when {
        actualChar == null ->
            (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
        actualChar.isLetter() -> {
            val (infix, afterInfix) = handleIdentifier(actualOnLine)
            val (part, next) = handleSubexpression(afterInfix.nextActual)
            handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
        }
        actualChar.isSymbolPart() -> {
            val (op, afterOp) = handleSymbol(state)
            if (afterOp.char?.isWhitespace() == false)
                INVALID_PREFIX_OPERATOR_ERROR throwAt afterOp.pos
            when (op.value) {
                ":" -> {
                    val (type, next) = handleType(afterOp.actual)
                    handleScopedBinOps(
                        next, TypeExpr(last, type) at last.area.first..type.area.last, body
                    )
                }
                "->" -> {
                    val input = exprToDecPattern(last, INVALID_PARAMETER_ERROR)
                    val actualInput =
                        if (input.value is DecTuplePattern) input.value.values
                        else listOf(input)
                    val (lamExpr, next) = handleInScopeExpression(afterOp.actual)
                    handleScopedBinOps(
                        next, LambdaExpr(actualInput, lamExpr) at last.area.first..lamExpr.area.last, body
                    )
                }
                "<-" -> INVALID_LEFT_ARROW_ERROR throwAt op.area
                "==" -> INVALID_ASSIGNMENT_ERROR throwAt op.area
                else -> {
                    val (part, next) = handleSubexpression(afterOp.actual)
                    handleScopedBinOps(next.actual, part, body + (last to op.map(::SymbolOp)))
                }
            }
        }
        actualChar == '{' -> {
            val (scope, next) = handleScope(actualOnLine.nextActual, actualOnLine.pos, false)
            handleScopedBinOps(next, CallExpr(last, listOf(scope)) at last.area.first..scope.area.last, body)
        }
        else -> {
            val maybeCurly = actualOnLine.actual
            if (maybeCurly.char == '{') {
                val (scope, next) = handleScope(maybeCurly.nextActual, maybeCurly.pos, false)
                handleScopedBinOps(
                    next, CallExpr(last, listOf(scope)) at last.area.first..scope.area.last, body
                )
            } else (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
        }
    }
}

fun handleFirstBinOp(
    state: ParseState,
    last: PExpr,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): Pair<PStatement, ParseState> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (afterOp.char.isAfterPostfix())
        handleScopedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, body)
    else {
        val (sub, next) = handleSubexpression(afterOp.actual)
        handleScopedBinOps(next, sub, body + (last to op.map(::SymbolOp)))
    }.mapFirst { it.map(::ExpressionStatement) }
} else {
    val actualOnLine = state.actualOrBreak
    val actualChar = actualOnLine.char
    when {
        actualChar == null ->
            (if (body.isEmpty()) last.map(::ExpressionStatement)
            else ExpressionStatement(
                BinaryOpsExpr(body, last)
            ) at body.first().first.area.first..last.area.last) to state
        actualChar.isLetter() -> {
            val (infix, afterInfix) = handleIdentifier(actualOnLine)
            val (part, next) = handleSubexpression(afterInfix.nextActual)
            handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
                .mapFirst { it.map(::ExpressionStatement) }
        }
        actualChar.isSymbolPart() -> {
            val (op, afterOp) = handleSymbol(state)
            if (afterOp.char?.isWhitespace() == false)
                INVALID_PREFIX_OPERATOR_ERROR throwAt afterOp.pos
            when (op.value) {
                ":" -> {
                    val (type, next) = handleType(afterOp.actual)
                    handleScopedBinOps(next, TypeExpr(last, type) at last.area.first..type.area.last, body)
                        .mapFirst { it.map(::ExpressionStatement) }
                }
                "->" -> {
                    val input = exprToDecPattern(last, INVALID_PARAMETER_ERROR)
                    val actualInput =
                        if (input.value is DecTuplePattern) input.value.values
                        else listOf(input)
                    val (lamExpr, next) = handleScope(afterOp.actual, last.area.first, true)
                    handleScopedBinOps(
                        next, LambdaExpr(actualInput, lamExpr) at last.area.first..lamExpr.area.last, body
                    ).mapFirst { it.map(::ExpressionStatement) }
                }
                "<-" -> {
                    val pattern = exprToDecPattern(last, INVALID_PARAMETER_ERROR)
                    val actualPattern = if (pattern.value is DecTuplePattern) pattern.value.values else listOf(pattern)
                    val (fromExpr, afterState) = handleInScopeExpression(afterOp.actual)
                    val sepState = afterState.actual
                    if (!sepState.char.isExpressionSeparator())
                        MISSING_EXPRESSION_SEPARATOR_ERROR throwAt sepState.pos
                    val actualAfterState = sepState.nextActual
                    val (scope, next) = handleScope(sepState.nextActual, actualAfterState.pos, false)
                    val area = last.area.first..scope.area.last
                    ExpressionStatement(
                        BinaryOpsExpr(
                            listOf(fromExpr to (SymbolOp(">>=") at sepState.pos)),
                            LambdaExpr(actualPattern, scope) at area
                        )
                    ) at area to next
                }
                "==" -> if (last.value is CallExpr) {
                    if (last.value.expr.value !is PathExpr) INVALID_FUNCTION_NAME throwAt last.value.expr.area
                    if (last.value.expr.value.parts.size == 1) INVALID_FUNCTION_NAME throwAt last.value.expr.area
                    val name = last.value.expr.value.parts.first()
                    val (funExpr, next) = handleInScopeExpression(afterOp.actual)
                    FunctionAssignment(name, last.value.params, funExpr, false) at
                                        last.area.first..funExpr.area.last to next
                } else {
                    val pattern = exprToDecPattern(last, INVALID_DESTRUCTURED_DECLARATION_ERROR)
                    val (valueExpr, next) = handleInScopeExpression(afterOp.actual)
                    ConstAssignment(pattern, valueExpr, false) at
                            last.area.first..valueExpr.area.last to next
                }
                else -> {
                    val (part, next) = handleSubexpression(afterOp.actual)
                    handleScopedBinOps(next.actual, part, body + (last to op.map(::SymbolOp)))
                        .mapFirst { it.map(::ExpressionStatement) }
                }
            }
        }
        actualChar == '{' -> {
            val (scope, next) = handleScope(actualOnLine.nextActual, actualOnLine.pos, false)
            handleScopedBinOps(next, CallExpr(last, listOf(scope)) at last.area.first..scope.area.last, body)
                .mapFirst { it.map(::ExpressionStatement) }
        }
        else -> {
            val maybeCurly = actualOnLine.actual
            if (maybeCurly.char == '{') {
                val (scope, next) = handleScope(maybeCurly.nextActual, maybeCurly.pos, false)
                handleScopedBinOps(
                    next, CallExpr(last, listOf(scope)) at last.area.first..scope.area.last, body
                ).mapFirst { it.map(::ExpressionStatement) }
            } else (if (body.isEmpty()) last.map(::ExpressionStatement)
            else ExpressionStatement(BinaryOpsExpr(body, last)) at body.first().first.area.first..last.area.last) to state
        }
    }
}

fun handleSubexpression(state: ParseState): Pair<PExpr, ParseState> {
    val char = state.char
    fun Char.eq() = this == char
    return when {
        char == null -> MISSING_EXPRESSION_ERROR throwAt state.lastPos
        char.isLetter() -> {
            val (ident, next) = handleIdentifier(state)
            when (ident.value) {
                "_" -> WildcardExpr at state.lastPos to next
                "mut" -> {
                    val afterMut = next.nextActual
                    if (afterMut.char == '[')
                        handleList(afterMut.nextActual, afterMut.pos, true, emptyList(), emptyList())
                    else handlePath(next, ident, emptyList())
                }
                "when" -> {
                    val afterWhenState = next.actual
                    if (afterWhenState.char == '{') handleWhen(
                        afterWhenState.nextActual, PathExpr(listOf("True" at next.pos)) at next.pos,
                        afterWhenState.pos, emptyList()
                    ) else {
                        val (comparing, afterExpr) = handleInlinedExpression(
                            afterWhenState,
                            excludeCurly = true,
                            excludeArrow = true
                        )
                        val bracketState = afterExpr.actual
                        if (bracketState.char == '{') handleWhen(
                            bracketState.nextActual, comparing, bracketState.pos, emptyList()
                        ) else MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR throwAt bracketState.pos
                    }
                }
                "let" -> {
                    val (pattern, afterState) = handleInScopeExpression(next.actual)
                    val equalsState = afterState.actual
                    if (equalsState.char == '=') {
                        val (expr, returnState) = handleInScopeExpression(equalsState.nextActual)
                        DecExpr(pattern, expr) at state.pos..expr.area.last to returnState
                    } else DecExpr(pattern, null) at state.pos..pattern.area.last to afterState
                }
                else -> handlePath(next, ident, emptyList())
            }
        }
        '0'.eq() -> when (state.nextChar) {
            'x', 'X' -> handleHexNumber(
                state + 2,
                state.pos,
                StringBuilder()
            )
            'b', 'B' -> handleBinaryNumber(
                state + 2,
                state.pos,
                StringBuilder()
            )
            else -> handleNumber(state, state.pos, StringBuilder())
        }
        char in '1'..'9' -> handleNumber(
            state,
            state.pos,
            StringBuilder()
        )
        '"'.eq() -> if (state.nextChar == '"' && (state + 2).char == '"') {
            handleMultiLineString(
                state + 3,
                state.pos,
                emptyList(),
                StringBuilder()
            )
        } else handleSingleLineString(
            state.next,
            state.pos,
            emptyList(),
            StringBuilder()
        )
        '\''.eq() -> handleChar(state.next)
        '('.eq() -> handleParenthesizedExpr(state.nextActual, state.pos)
        '['.eq() -> if (state.nextChar == '|') {
            handleArray(state.nextActual, state.pos, emptyList(), emptyList())
        } else handleList(state.nextActual, state.pos, false, emptyList(), emptyList())
        '{'.eq() -> handleScope(state.next, state.pos, false)
        char.isSymbolPart() -> {
            val (symbol, exprState) = handleSymbol(state)
            val (expr, next) = handleSubexpression(exprState)
            PrefixOpExpr(symbol, expr) at state.pos..expr.area.last to next
        }
        else -> INVALID_EXPRESSION_ERROR throwAt state.pos
    }
}

tailrec fun handleParams(
    state: ParseState,
    start: StringPos,
    expressions: List<PExpr> = emptyList()
): Pair<Positioned<List<PExpr>>, ParseState> = if (state.char == ')')
    (if (expressions.isEmpty()) listOf(UnitExpr at start..state.pos) else expressions) at
            start..state.pos to state.next
else {
    val (expr, afterState) = handleInlinedExpression(state, true)
    val symbolState = afterState.actual
    when (symbolState.char) {
        ')' -> expressions + expr at start..symbolState.pos to symbolState.next
        ',' -> handleParams(symbolState.nextActual, start, expressions + expr)
        else -> UNCLOSED_PARENTHESIS_ERROR throwAt symbolState.pos
    }
}

tailrec fun handlePath(
    state: ParseState,
    last: PString,
    path: List<PString>
): Pair<Positioned<PathExpr>, ParseState> = if (state.char == '.') {
    val identState = state.next
    if (identState.char?.isLetter() != true) INVALID_PATH_ERROR throwAt state.pos
    val (ident, next) = handleIdentifier(identState)
    handlePath(next, ident, path + last)
} else PathExpr(path + last) at (path.firstOrNull() ?: last).area.first..last.area.last to state

tailrec fun handleParenthesizedExpr(
    state: ParseState,
    start: StringPos,
    expressions: List<PExpr> = emptyList()
): Pair<PExpr, ParseState> = if (state.char == ')') when (expressions.size) {
    0 -> UnitExpr at start..state.pos
    1 -> expressions.first()
    else -> TupleExpr(expressions) at start..state.pos
} to state.next else {
    val (expr, afterState) = handleInlinedExpression(state, true)
    val symbolState = afterState.actual
    when (symbolState.char) {
        ')' -> (if (expressions.isEmpty()) expr else TupleExpr(expressions + expr) at start..symbolState.pos) to symbolState.next
        ',' -> handleParenthesizedExpr(symbolState.nextActual, start, expressions + expr)
        else -> UNCLOSED_PARENTHESIS_ERROR throwAt symbolState.pos
    }
}

tailrec fun handleList(
    state: ParseState,
    start: StringPos,
    mutable: Boolean,
    expressions: List<PExpr>,
    sections: List<Positioned<ListExpr>>,
    lastStart: StringPos = start
): Pair<PExpr, ParseState> = if (state.char == ']') {
    ListExpr(
        if (sections.isEmpty()) expressions
        else sections + (ListExpr(expressions, mutable) at lastStart..state.pos), mutable
    ) at start..state.pos to state.next
} else {
    val (expr, afterState) = handleInlinedExpression(state, true)
    val symbolState = afterState.actual
    val newExpressions = expressions + expr
    when (symbolState.char) {
        ']' ->
            ListExpr(
                if (sections.isEmpty()) newExpressions else sections + (ListExpr(newExpressions, mutable) at
                        lastStart..symbolState.pos), mutable
            ) at start..symbolState.pos to symbolState.next
        ',' -> handleList(symbolState.nextActual, start, mutable, newExpressions, sections, lastStart)
        ';' -> handleList(
            symbolState.nextActual, start, mutable, emptyList(),
            sections + (ListExpr(expressions + expr, mutable) at lastStart..symbolState.pos),
            symbolState.pos
        )
        else -> UNCLOSED_SQUARE_BRACKET_ERROR throwAt symbolState.pos
    }
}

fun handleArray(
    state: ParseState,
    start: StringPos,
    expressions: List<PExpr>,
    sections: List<Positioned<ArrayExpr>>,
    lastStart: StringPos = start
): Pair<PExpr, ParseState> = if (state.char == '|' && state.nextChar == ']') {
    ArrayExpr(
        if (sections.isEmpty()) expressions else sections + (ArrayExpr(expressions) at
                lastStart..state.nextPos)
    ) at start..state.nextPos to state + 2
} else {
    val (expr, afterState) = handleInlinedExpression(state, true)
    val symbolState = afterState.actual
    val symbol = symbolState.char
    val newExpressions = expressions + expr
    when {
        symbol == '|' && symbolState.nextChar == ']' ->
            ArrayExpr(
                if (sections.isEmpty()) newExpressions else sections + (ArrayExpr(
                    newExpressions
                ) at lastStart..symbolState.nextPos)
            ) at start..symbolState.nextPos to symbolState + 2
        symbol == ',' -> handleArray(symbolState.nextActual, start, newExpressions, sections, lastStart)
        symbol == ';' -> handleArray(
            symbolState.nextActual, start, emptyList(),
            sections + (ArrayExpr(expressions + expr) at lastStart..symbolState.pos), symbolState.pos
        )
        else -> UNCLOSED_SQUARE_BRACKET_ERROR throwAt symbolState.pos
    }
}

tailrec fun handleWhen(
    state: ParseState,
    comparing: PExpr,
    start: StringPos,
    branches: List<Pair<PExpr, PExpr>>
): Pair<PExpr, ParseState> = when (state.char) {
    ')' -> WhenExpr(comparing, branches) at start..state.pos to state.next
    ',', ';' -> handleWhen(state.nextActual, comparing, start, branches)
    else -> {
        val (pattern, arrowState) = handleInScopeExpression(state)
        if (arrowState.char != '-' || arrowState.nextChar != '>') MISSING_ARROW_ERROR throwAt arrowState.pos
        val (result, afterState) = handleInScopeExpression((arrowState + 2).nextActual)
        val sepState = afterState.actualOrBreak
        if (sepState.char.isExpressionSeparator())
            handleWhen(sepState.nextActual, comparing, start, branches + (pattern to result))
        else UNCLOSED_WHEN_ERROR throwAt sepState.pos
    }
}