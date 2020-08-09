package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

sealed class Expression
typealias PExpr = Positioned<Expression>

data class IdentExpr(val name: String) : Expression()
data class OpRefExpr(val symbol: String) : Expression()

data class CallExpr(
    val expr: PExpr,
    val params: List<PExpr>
) : Expression()

data class LambdaExpr(
    val params: List<Pair<PString, PType?>>,
    val scope: ExpressionScope
) : Expression()

sealed class Condition
data class ExprCondition(val expr: PExpr) : Condition()
data class DecCondition(val pattern: PPattern, val expr: PExpr) : Condition()

data class IfExpr(
    val cond: List<Condition>,
    val ifTrue: ExpressionScope,
    val ifFalse: ExpressionScope
) : Expression()

data class WhenExpr(
    val comparing: PExpr,
    val branches: List<Pair<PPattern, PExpr>>
) : Expression()

data class ForExpr(
    val name: PString,
    val iterable: PExpr,
    val body: ExpressionScope
) : Expression()

data class ScopeExpr(val scope: ExpressionScope) : Expression()

data class ByteExpr(val value: Byte) : Expression()
data class ShortExpr(val value: Short) : Expression()
data class IntExpr(val value: Int) : Expression()
data class LongExpr(val value: Long) : Expression()
data class FloatExpr(val value: Float) : Expression()
data class DoubleExpr(val value: Double) : Expression()
data class CharExpr(val value: Char) : Expression()
data class StringExpr(val string: String) : Expression()
data class BoolExpr(val value: Boolean) : Expression()

object UnitExpr : Expression()
object NullExpr : Expression()

data class ListExpr(val components: List<PExpr>) : Expression()
data class ArrayExpr(val components: List<PExpr>) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String) : BinOp()

val plus = SymbolOp("+")

data class PrefixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class BinaryOpsExpr(val body: List<Pair<PExpr, PBinOp>>, val end: PExpr) : Expression()

object ContinueExpr : Expression()
data class BreakExpr(val expr: PExpr) : Expression()
data class ReturnExpr(val expr: PExpr) : Expression()

fun requireSymbol(state: ParseState, symbol: String): ParseState {
    val (pSymbol, next) = handleSymbol(state)
    return if (symbol == pSymbol.value) next else unexpectedSymbolError(symbol) throwAt pSymbol.area
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleExprScope(
    state: ParseState,
    start: StringPos,
    statements: List<PSStatement> = emptyList()
): Pair<Positioned<ExpressionScope>, ParseState> {
    val char = state.char
    return when {
        char == null -> unclosedScopeError throwAt state.pos
        char == '}' -> ExpressionScope(statements) at start..state.pos to state.next
        char.isSeparator() -> handleExprScope(state.nextActual, start, statements)
        char.isLetter() -> {
            val (ident, afterIdent) = handleIdentifier(state)
            val (statement, afterState) = when (ident.value) {
                "val" -> handleDeclaration(afterIdent.actual, state.pos, true)
                "var" -> handleDeclaration(afterIdent.actual, state.pos, false)
                else -> {
                    val (sub, next) = handleSubexpression(state)
                    handleScopedBinOps(next.actualOrBreak, sub).mapFirst { it.map(::ExprStatement) }
                }
            }
            val sepState = afterState.actualOrBreak
            if (sepState.char.isExpressionSeparator())
                handleExprScope(sepState.nextActual, start, statements + statement)
            else missingExpressionSeparatorError throwAt sepState.pos
        }
        else -> {
            val (expr, afterState) = handleInScopeExpression(state)
            val sepState = afterState.actualOrBreak
            if (sepState.char.isExpressionSeparator())
                handleExprScope(sepState.nextActual, start, statements + expr.map(::ExprStatement))
            else missingExpressionSeparatorError throwAt sepState.pos
        }
    }
}

fun handleInlinedExpression(
    state: ParseState,
    excludeCurly: Boolean = false
): ParseResult<PExpr> {
    val (firstPart, next) = handleSubexpression(state)
    return handleInlinedBinOps(next, firstPart, excludeCurly)
}

fun handleInScopeExpression(state: ParseState): Pair<PExpr, ParseState> {
    val (firstPart, next) = handleSubexpression(state)
    return handleScopedBinOps(next, firstPart)
}

tailrec fun handleInlinedBinOps(
    state: ParseState,
    last: PExpr,
    excludeCurly: Boolean,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): ParseResult<PExpr> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    val symbol = op.value
    if (!(symbol.endsWith('.') && symbol.length <= 2) && afterOp.char.isAfterPostfix()) {
        handleInlinedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, excludeCurly, body)
    } else {
        val (sub, next) = handleSubexpression(afterOp.actual)
        handleInlinedBinOps(next, sub, excludeCurly, body + (last to op.map(::SymbolOp)))
    }
} else {
    val actual = state.actual
    val actualChar = actual.char
    when {
        actualChar == null ->
            (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) succTo state
        actualChar.isLetter() -> {
            val (infix, afterInfix) = handleIdentifier(actual)
            if (infix.value in keywords) {
                (if (body.isEmpty()) last
                else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) succTo state
            } else {
                val (part, next) = handleSubexpression(afterInfix.nextActual)
                handleInlinedBinOps(next, part, excludeCurly, body + (last to infix.map(::InfixCall)))
            }
        }
        actualChar == '`' -> {
            val (infix, afterInfix) = handleBacktickedIdentifier(actual.next)
            val (part, next) = handleSubexpression(afterInfix.nextActual)
            handleInlinedBinOps(next, part, excludeCurly, body + (last to infix.map(::InfixCall)))
        }
        actualChar.isSymbolPart() -> {
            val (op, afterOp) = handleSymbol(state)
            val symbol = op.value
            if (!(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false)
                invalidPrefixOperatorError throwAt afterOp.pos
            when (symbol) {
                "->" ->
                    (if (body.isEmpty()) last
                    else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) succTo state
                else -> {
                    val (part, next) = handleSubexpression(afterOp.actual)
                    handleInlinedBinOps(next.actual, part, excludeCurly, body + (last to op.map(::SymbolOp)))
                }
            }
        }
        actualChar == '(' -> {
            val (params, next) = handleParams(actual.nextActual, actual.pos)
            handleInlinedBinOps(
                next, CallExpr(last, params.value) at last.area.first..params.area.last,
                excludeCurly, body
            )
        }
        !excludeCurly && actualChar == '{' -> TODO()
        else ->
            (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) succTo state
    }
}

tailrec fun handleScopedBinOps(
    state: ParseState,
    last: PExpr,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): Pair<PExpr, ParseState> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    val symbol = op.value
    if (!(symbol.endsWith('.') && symbol.length <= 2) && afterOp.char.isAfterPostfix()) {
        handleScopedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, body)
    } else {
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
            if (infix.value in keywords) {
                (if (body.isEmpty()) last
                else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
            } else {
                val (part, next) = handleSubexpression(afterInfix.nextActual)
                handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
            }
        }
        actualChar == '`' -> {
            val (infix, afterInfix) = handleBacktickedIdentifier(actualOnLine.next)
            val (part, next) = handleSubexpression(afterInfix.nextActual)
            handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
        }
        actualChar.isSymbolPart() -> {
            val (op, afterOp) = handleSymbol(state)
            val symbol = op.value
            if (!(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false)
                invalidPrefixOperatorError throwAt afterOp.pos
            when (symbol) {
                "->" ->
                    (if (body.isEmpty()) last
                    else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
                else -> {
                    val (part, next) = handleSubexpression(afterOp.actual)
                    handleScopedBinOps(next.actual, part, body + (last to op.map(::SymbolOp)))
                }
            }
        }
        actualChar == '{' -> TODO()
        else -> {
            val maybeCurly = actualOnLine.actual
            if (maybeCurly.char == '{') TODO() else (if (body.isEmpty()) last
            else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to state
        }
    }
}

fun handleSubexpression(state: ParseState): Pair<PExpr, ParseState> {
    val char = state.char
    fun Char.eq() = this == char
    return when {
        char == null -> missingExpressionError throwAt state.lastPos
        char.isLetter() -> {
            val (ident, next) = handleIdentifier(state)
            when (ident.value) {
                "_" -> WildcardExpr at state.lastPos to next
                "when" -> {
                    val afterWhenState = next.actual
                    if (afterWhenState.char == '{') handleWhen(
                        afterWhenState.nextActual, IdentExpr("True") at next.pos,
                        afterWhenState.pos, emptyList()
                    ) else {
                        val (comparing, afterExpr) = handleInlinedExpression(afterWhenState, true)
                        val bracketState = afterExpr.actual
                        if (bracketState.char == '{') handleWhen(
                            bracketState.nextActual, comparing, bracketState.pos, emptyList()
                        ) else missingCurlyBracketAfterWhenError throwAt bracketState.pos
                    }
                }
                else -> ident.map(::IdentExpr) to next
            }
        }
        '`'.eq() -> handleBacktickedIdentifier(state.next).mapFirst { it.map(::IdentExpr) }
        '0'.eq() -> when (state.nextChar) {
            'x', 'X' -> handleHexNumber(state + 2, state.pos, StringBuilder())
            'b', 'B' -> handleBinaryNumber(state + 2, state.pos, StringBuilder())
            else -> handleNumber(state, state.pos, StringBuilder())
        }
        char in '1'..'9' -> handleNumber(state, state.pos, StringBuilder())
        '"'.eq() -> if (state.nextChar == '"' && (state + 2).char == '"') {
            handleMultiLineString(state + 3, state.pos, emptyList(), StringBuilder())
        } else handleSingleLineString(state.next, state.pos, emptyList(), StringBuilder())
        '\''.eq() -> handleChar(state.next)
        '('.eq() -> handleParenthesizedExpr(state.nextActual, state.pos)
        '['.eq() -> if (state.nextChar == '|') {
            handleArray(state.nextActual, state.pos, emptyList(), emptyList())
        } else handleList(state.nextActual, state.pos, emptyList(), emptyList())
        '{'.eq() -> TODO()
        char.isSymbolPart() -> {
            val (symbol, exprState) = handleSymbol(state)
            if (exprState.char.isAfterPostfix()) symbol.map(::OpRefExpr) to exprState else {
                val (expr, next) = handleSubexpression(exprState)
                PrefixOpExpr(symbol, expr) at state.pos..expr.area.last to next
            }
        }
        else -> invalidExpressionError throwAt state.pos
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
        else -> unclosedParenthesisError throwAt symbolState.pos
    }
}

fun handleParenthesizedExpr(
    state: ParseState,
    startPos: StringPos
): Pair<PExpr, ParseState> = if (state.char == ')') UnitExpr at startPos..state.pos to state.next else {
    val (expr, after) = handleInlinedExpression(state)
    val paren = after.actual
    if (paren.char == ')') unclosedParenthesisError throwAt paren.pos
    expr to paren.next
}

tailrec fun handleList(
    state: ParseState,
    start: StringPos,
    expressions: List<PExpr>,
    sections: List<Positioned<ListExpr>>,
    lastStart: StringPos = start
): Pair<PExpr, ParseState> = if (state.char == ']') {
    ListExpr(
        if (sections.isEmpty()) expressions
        else sections + (ListExpr(expressions) at lastStart..state.pos)
    ) at start..state.pos to state.next
} else {
    val (expr, afterState) = handleInlinedExpression(state, true)
    val symbolState = afterState.actual
    val newExpressions = expressions + expr
    when (symbolState.char) {
        ']' ->
            ListExpr(
                if (sections.isEmpty()) newExpressions else sections + (ListExpr(newExpressions) at
                        lastStart..symbolState.pos)
            ) at start..symbolState.pos to symbolState.next
        ',' -> handleList(symbolState.nextActual, start, newExpressions, sections, lastStart)
        ';' -> handleList(
            symbolState.nextActual, start, emptyList(),
            sections + (ListExpr(expressions + expr) at lastStart..symbolState.pos),
            symbolState.pos
        )
        else -> unclosedSquareBacketError throwAt symbolState.pos
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
        else -> unclosedSquareBacketError throwAt symbolState.pos
    }
}

fun handleIf(state: ParseState, start: StringPos)

tailrec fun handleConditions(
    state: ParseState,
    previous: List<Condition>
): Pair<List<Condition>, ParseState> {
    val (cond, afterCond) = if (state.char?.isLetter() == true) {
        val (ident, afterIdent) = handleIdentifier(state)
        when (ident.value) {
            "val" -> {
                val (pattern, afterPattern) = handlePattern(afterIdent.actual, DeclarationType.VAL)
                val exprStart = requireSymbol(afterPattern.actual, "=").actual
                val (expr, afterExpr) = handleInlinedExpression(exprStart, true)
                DecCondition(pattern, expr) to afterExpr
            }
            "var" -> {
                val (pattern, afterPattern) = handlePattern(afterIdent.actual, DeclarationType.VAR)
                val exprStart = requireSymbol(afterPattern.actual, "=").actual
                val (expr, afterExpr) = handleInlinedExpression(exprStart, true)
                DecCondition(pattern, expr) to afterExpr
            }
            else -> {
                val (expr, afterExpr) = handleInlinedBinOps(afterIdent, ident.map(::IdentExpr), true)
                ExprCondition(expr) to afterExpr
            }
        }
    } else {
        val (expr, afterExpr) = handleInlinedExpression(state, true)
        ExprCondition(expr) to afterExpr
    }
    val maybeComma = afterCond.actual
    if (maybeComma.char == ',') handleConditions(maybeComma.nextActual, previous + cond)
    else (previous + cond) to maybeComma
}

tailrec fun handleWhen(
    state: ParseState,
    comparing: PExpr,
    start: StringPos,
    branches: List<Pair<PPattern, PExpr>>
): Pair<PExpr, ParseState> = when (state.char) {
    ')' -> WhenExpr(comparing, branches) at start..state.pos to state.next
    ';' -> handleWhen(state.nextActual, comparing, start, branches)
    else -> {
        val (pattern, arrowState) = handlePattern(state, DeclarationType.NONE)
        val maybeCurly = requireSymbol(arrowState.actual, "->").actual
        val (result, afterState) = if (maybeCurly.char == '{') {
            handleExprScope(maybeCurly.nextActual, maybeCurly.pos).mapFirst { it.map(::ScopeExpr) }
        } else handleInScopeExpression((arrowState + 2).nextActual)
        val sepState = afterState.actualOrBreak
        if (sepState.char.isExpressionSeparator())
            handleWhen(sepState.nextActual, comparing, start, branches + (pattern to result))
        else unclosedWhenError throwAt sepState.pos
    }
}