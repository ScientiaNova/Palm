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
    val scope: ExprScope
) : Expression()

sealed class Condition
data class ExprCondition(val expr: PExpr) : Condition()
data class DecCondition(val pattern: PPattern, val expr: PExpr) : Condition()

data class IfExpr(
    val cond: List<Condition>,
    val ifTrue: ExprScope,
    val ifFalse: ExprScope
) : Expression()

typealias WhenBranch = List<Pair<PPattern, PExpr>>

data class WhenExpr(
    val comparing: PExpr,
    val branches: WhenBranch
) : Expression()

data class ForExpr(
    val name: PString,
    val iterable: PExpr,
    val body: ExprScope
) : Expression()

data class ScopeExpr(val scope: ExprScope) : Expression()

data class ByteExpr(val value: Byte) : Expression()
data class ShortExpr(val value: Short) : Expression()
data class IntExpr(val value: Int) : Expression()
data class LongExpr(val value: Long) : Expression()
data class FloatExpr(val value: Float) : Expression()
data class DoubleExpr(val value: Double) : Expression()
data class CharExpr(val value: Char) : Expression()
data class StringExpr(val string: String) : Expression()
data class BoolExpr(val value: Boolean) : Expression()

val trueExpr = BoolExpr(true)
val falseExpr = BoolExpr(false)

object NullExpr : Expression()

data class ListExpr(val components: List<PExpr>) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String) : BinOp()

val plus = SymbolOp("+")

typealias OpenOpsList = List<Pair<PExpr, PBinOp>>

data class PrefixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class BinaryOpsExpr(val body: OpenOpsList, val end: PExpr) : Expression()

object ContinueExpr : Expression()
data class BreakExpr(val expr: PExpr?) : Expression()
data class ReturnExpr(val expr: PExpr?) : Expression()

inline fun ParseState.requireSymbol(symbol: String, error: (String) -> PalmError = ::unexpectedSymbolError): ParseBool {
    val (pSymbol, next) = handleSymbol(this)
    return if (symbol == pSymbol.value) parseTrue(next) else error(symbol) errAt pSymbol.area
}

inline fun <T> ParseState.requireChar(symbol: Char, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (char == symbol) then(next) else error errAt pos

inline fun <T> ParseState.requireChar(
    predicate: (Char?) -> Boolean,
    error: PalmError,
    then: (ParseState) -> ParseResult<T>
) = if (predicate(char)) then(next) else error errAt pos

fun PString.startExpr(
    afterIdent: ParseState,
    scoped: Boolean,
    excludeCurly: Boolean = scoped
) = handleIdentSubexpr(this, afterIdent, scoped).flatMap { firstExpr, afterFirst ->
    if (scoped) handleScopedBinOps(afterFirst, firstExpr, emptyList())
    else handleInlinedBinOps(afterFirst, firstExpr, excludeCurly, emptyList())
}

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleExprScope(
    state: ParseState,
    start: StringPos,
    statements: List<PSStatement> = emptyList()
): ParseResult<Positioned<ExprScope>> = when (state.char) {
    null -> unclosedScopeError errAt state.pos
    '}' -> ExprScope(statements) at start..state.pos succTo state.next
    in separatorChars -> handleExprScope(state.nextActual, start, statements)
    in identStartChars -> {
        val (ident, afterIdent) = handleIdent(state)
        when (ident.value) {
            "val" -> handleDeclaration(afterIdent.actual, state.pos, true)
            "var" -> handleDeclaration(afterIdent.actual, state.pos, false)
            else -> ident.startExpr(afterIdent, true).map { it.map(::ExprStatement) }
        }.flatMap { statement, afterState ->
            val sepState = afterState.actualOrBreak
            if (sepState.char.isExpressionSeparator())
                handleExprScope(sepState.nextActual, start, statements + statement)
            else missingExpressionSeparatorError errAt sepState.pos
        }
    }
    else -> handleScopedExpr(state).flatMap { expr, afterState ->
        afterState.actualOrBreak.requireChar(
            Char?::isExpressionSeparator, missingExpressionSeparatorError
        ) { afterSep ->
            handleExprScope(afterSep.actual, start, statements + expr.map(::ExprStatement))
        }
    }
}

fun handleInlinedExpr(
    state: ParseState,
    excludeCurly: Boolean = false
): ParseResult<PExpr> = handleSubexpr(state, false).flatMap { firstPart, next ->
    handleInlinedBinOps(next, firstPart, excludeCurly)
}

fun handleScopedExpr(state: ParseState): ParseResult<PExpr> = handleSubexpr(state, true).flatMap { firstPart, next ->
    handleScopedBinOps(next, firstPart)
}

tailrec fun handleInlinedBinOps(
    state: ParseState,
    last: PExpr,
    excludeCurly: Boolean,
    body: OpenOpsList = emptyList()
): ParseResult<PExpr> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (op.value.isPostfixOp(afterOp)) {
        handleInlinedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, excludeCurly, body)
    } else handleSubexpr(afterOp.actual, false).flatMap { sub, next ->
        return handleInlinedBinOps(next, sub, excludeCurly, body + (last to op.map(::SymbolOp)))
    }
} else {
    val actual = state.actual
    when (actual.char) {
        null -> finishBinOps(body, last, state)
        in identStartChars -> {
            val (infix, afterInfix) = handleIdent(actual)
            if (infix.value in keywords) finishBinOps(body, last, state)
            else handleSubexpr(afterInfix.nextActual, false).flatMap { part, next ->
                return handleInlinedBinOps(next, part, excludeCurly, body + (last to infix.map(::InfixCall)))
            }
        }
        '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
            handleSubexpr(afterInfix.nextActual, false).flatMap<PExpr, PExpr> { part, next ->
                return handleInlinedBinOps(next, part, excludeCurly, body + (last to infix.map(::InfixCall)))
            }
        }
        in symbolChars -> {
            val (op, afterOp) = handleSymbol(state)
            val symbol = op.value
            if (!(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false)
                invalidPrefixOperatorError errAt afterOp.pos
            when (symbol) {
                "->" -> finishBinOps(body, last, state)
                else -> handleSubexpr(afterOp.actual, false).flatMap { part, next ->
                    return handleInlinedBinOps(next.actual, part, excludeCurly, body + (last to op.map(::SymbolOp)))
                }
            }
        }
        '(' -> handleParams(actual.nextActual, actual.pos).flatMap { params, next ->
            return handleInlinedBinOps(
                next, CallExpr(last, params.value) at last.area.first..params.area.last,
                excludeCurly, body
            )
        }
        '{' -> if (excludeCurly) finishBinOps(body, last, state) else TODO()
        else -> finishBinOps(body, last, state)
    }
}

tailrec fun handleScopedBinOps(
    state: ParseState,
    last: PExpr,
    body: OpenOpsList = emptyList()
): ParseResult<PExpr> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (op.value.isPostfixOp(afterOp)) {
        handleScopedBinOps(afterOp, PostfixOpExpr(op, last) at last.area.first..op.area.last, body)
    } else handleSubexpr(afterOp.actual, true).flatMap { sub, next ->
        return handleScopedBinOps(next, sub, body + (last to op.map(::SymbolOp)))
    }
} else {
    val actualOnLine = state.actualOrBreak
    when (actualOnLine.char) {
        null -> finishBinOps(body, last, state)
        in identStartChars -> {
            val (infix, afterInfix) = handleIdent(actualOnLine)
            if (infix.value in keywords) finishBinOps(body, last, state)
            else handleSubexpr(afterInfix.nextActual, true).flatMap { part, next ->
                return handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
            }
        }
        '`' -> handleBacktickedIdent(actualOnLine.next).flatMap { infix, afterInfix ->
            handleSubexpr(afterInfix.nextActual, true).flatMap<PExpr, PExpr> { part, next ->
                return handleScopedBinOps(next, part, body + (last to infix.map(::InfixCall)))
            }
        }
        in symbolChars -> {
            val (op, afterOp) = handleSymbol(state)
            val symbol = op.value
            if (!(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false)
                invalidPrefixOperatorError errAt afterOp.pos
            else when (symbol) {
                "->" -> finishBinOps(body, last, state)
                else -> handleSubexpr(afterOp.actual, true).flatMap { part, next ->
                    return handleScopedBinOps(next.actual, part, body + (last to op.map(::SymbolOp)))
                }
            }
        }
        else -> {
            val maybeCurly = actualOnLine.actual
            if (maybeCurly.char == '{') TODO() else finishBinOps(body, last, state)
        }
    }
}

private fun String.isPostfixOp(afterOp: ParseState) =
    !(endsWith('.') && length <= 2) && afterOp.char.isAfterPostfix()

private fun finishBinOps(
    body: OpenOpsList,
    last: PExpr,
    state: ParseState
) = (if (body.isEmpty()) last else BinaryOpsExpr(body, last) at
        body.first().first.area.first..last.area.last) succTo state

fun handleSubexpr(state: ParseState, scoped: Boolean): ParseResult<PExpr> = when (state.char) {
    null -> missingExpressionError errAt state.pos
    in identStartChars -> {
        val (ident, next) = handleIdent(state)
        handleIdentSubexpr(ident, next, scoped)
    }
    '`' -> handleBacktickedIdent(state.next).map { it.map(::IdentExpr) }
    '0' -> when (state.nextChar) {
        'x', 'X' -> handleHexNumber(state + 2, state.pos, StringBuilder())
        'b', 'B' -> handleBinaryNumber(state + 2, state.pos, StringBuilder())
        else -> handleNumber(state, state.pos, StringBuilder())
    }
    in '1'..'9' -> handleNumber(state, state.pos, StringBuilder())
    '"' -> if (state.next.startWith("\"\"")) {
        handleMultiLineString(state + 3, state.pos, emptyList(), StringBuilder())
    } else handleSingleLineString(state.next, state.pos, emptyList(), StringBuilder())
    '\'' -> handleChar(state.next)
    '(' -> handleParenthesizedExpr(state.nextActual)
    '[' -> handleList(state.nextActual, state.pos, emptyList(), emptyList())
    '{' -> TODO()
    in symbolChars -> {
        val (symbol, exprState) = handleSymbol(state)
        if (exprState.char.isAfterPostfix()) symbol.map(::OpRefExpr) succTo exprState
        else handleSubexpr(exprState, scoped).flatMap { expr, next ->
            PrefixOpExpr(symbol, expr) at state.pos..expr.area.last succTo next
        }
    }
    else -> invalidExpressionError errAt state.pos
}


fun handleIdentSubexpr(
    ident: PString,
    next: ParseState,
    scoped: Boolean
): ParseResult<Positioned<Expression>> = when (ident.value) {
    "when" -> {
        val afterWhenState = next.actual
        if (afterWhenState.char == '{') handleWhen(
            afterWhenState.nextActual, trueExpr at next.pos,
            afterWhenState.pos, emptyList()
        ) else handleInlinedExpr(afterWhenState, true).flatMap { comparing, afterExpr ->
            afterExpr.actual.requireChar('{', missingCurlyAfterWhenError) { afterCurly ->
                handleWhen(afterCurly.actual, comparing, ident.area.first, emptyList())
            }
        }
    }
    "true" -> trueExpr at ident.area succTo next
    "false" -> falseExpr at ident.area succTo next
    "null" -> NullExpr at ident.area succTo next
    "continue" -> ContinueExpr at ident.area succTo next
    "return" -> handleContainerExpr(scoped, next, ident.area, ::ReturnExpr)
    "break" -> handleContainerExpr(scoped, next, ident.area, ::BreakExpr)
    else -> ident.map(::IdentExpr) succTo next
}

private inline fun handleContainerExpr(
    scoped: Boolean,
    next: ParseState,
    normalArea: StringArea,
    fn: (PExpr?) -> Expression
): ParseResult<Positioned<BreakExpr>> =
    (if (scoped) handleScopedExpr(next.actual) else handleInlinedExpr(next.actual, false))
        .biFlatMap({ expr, afterState -> fn(expr) at normalArea..expr.area.last succTo afterState })
        { fn(null) at normalArea succTo next }


tailrec fun handleParams(
    state: ParseState,
    start: StringPos,
    expressions: List<PExpr> = emptyList()
): ParseResult<Positioned<List<PExpr>>> = if (state.char == ')') {
    expressions at start..state.pos succTo state.next
} else handleInlinedExpr(state, true).flatMap { expr, afterState ->
    val symbolState = afterState.actual
    return when (symbolState.char) {
        ')' -> expressions + expr at start..symbolState.pos succTo symbolState.next
        ',' -> handleParams(symbolState.nextActual, start, expressions + expr)
        else -> unclosedParenthesisError errAt symbolState.pos
    }
}

fun handleParenthesizedExpr(
    state: ParseState
): ParseResult<PExpr> = if (state.char == ')') {
    emptyParenthesesOnExprError errAt state.pos
} else handleInlinedExpr(state).flatMap { expr, after ->
    after.actual.requireChar(')', unclosedParenthesisError) { next -> expr succTo next }
}


tailrec fun handleList(
    state: ParseState,
    start: StringPos,
    exprList: List<PExpr>,
    sections: List<Positioned<ListExpr>>,
    lastStart: StringPos = start
): ParseResult<PExpr> = if (state.char == ']') {
    finishList(sections, exprList, lastStart, state, start)
} else handleInlinedExpr(state, true).flatMap { expr, afterState ->
    val symbolState = afterState.actual
    val newExprList = exprList + expr
    return when (symbolState.char) {
        ']' -> finishList(sections, newExprList, lastStart, symbolState, start)
        ',' -> handleList(symbolState.nextActual, start, newExprList, sections, lastStart)
        ';' -> handleList(
            symbolState.nextActual, start, emptyList(),
            sections + (ListExpr(exprList + expr) at lastStart..symbolState.pos),
            symbolState.pos
        )
        else -> unclosedSquareBacketError errAt symbolState
    }
}

private fun finishList(
    sections: List<Positioned<ListExpr>>,
    expressions: List<PExpr>,
    lastStart: StringPos,
    endState: ParseState,
    start: StringPos
): ParseResult<PExpr> = ListExpr(
    if (sections.isEmpty()) expressions
    else sections + (ListExpr(expressions) at lastStart..endState.pos)
) at start..endState.pos succTo endState.next

fun handleIf(state: ParseState, start: StringPos)

tailrec fun handleConditions(
    state: ParseState,
    previous: List<Condition>
): ParseResult<List<Condition>> = if (state.char?.isLetter() == true) {
    val (ident, afterIdent) = handleIdent(state)
    when (ident.value) {
        "val" -> handlePatternCond(afterIdent, DeclarationType.VAL)
        "var" -> handlePatternCond(afterIdent, DeclarationType.VAR)
        else -> handleIdentSubexpr(ident, afterIdent, false).flatMap { firstExpr, binOpsState ->
            handleInlinedBinOps(binOpsState, firstExpr, true).flatMap { expr, afterExpr ->
                ExprCondition(expr) succTo afterExpr
            }
        }
    }
} else {
    handleInlinedExpr(state, true).flatMap { expr, afterExpr ->
        ExprCondition(expr) succTo afterExpr
    }
}.flatMap { cond, afterCond ->
    val maybeComma = afterCond.actual
    return if (maybeComma.char == ',') handleConditions(maybeComma.nextActual, previous + cond)
    else previous + cond succTo maybeComma
}

private fun handlePatternCond(
    afterIdent: ParseState,
    decType: DeclarationType
): ParseResult<DecCondition> = handlePattern(afterIdent.actual, decType, true).flatMap { pattern, afterPattern ->
    afterPattern.actual.requireSymbol("=").flatMap { afterEqState ->
        handleInlinedExpr(afterEqState.actual, true).flatMap { expr, afterExpr ->
            DecCondition(pattern, expr) succTo afterExpr
        }
    }
}

tailrec fun handleWhen(
    state: ParseState,
    comparing: PExpr,
    start: StringPos,
    branches: WhenBranch
): ParseResult<PExpr> = when (state.char) {
    ')' -> WhenExpr(comparing, branches) at start..state.pos succTo state.next
    ';' -> handleWhen(state.nextActual, comparing, start, branches)
    else -> handlePattern(state, DeclarationType.NONE, false).flatMap { pattern, arrowState ->
        arrowState.actual.requireSymbol("->").flatMapActual { maybeCurly ->
            if (maybeCurly.char == '{') {
                handleExprScope(maybeCurly.nextActual, maybeCurly.pos).map(PExprScope::toExpr)
            } else handleScopedExpr((arrowState + 2).nextActual).flatMap { result, afterState ->
                afterState.actualOrBreak.requireChar<PExpr>(
                    Char?::isExpressionSeparator, unclosedWhenError
                ) { afterStep ->
                    return handleWhen(afterStep.actual, comparing, start, branches + (pattern to result))
                }
            }
        }
    }
}