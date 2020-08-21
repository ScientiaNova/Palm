package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingDoubleQuoteError
import com.scientianova.palm.errors.unclosedMultilineStringError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleSingleLineString(
    state: ParseState,
    startPos: StringPos,
    parts: List<PExpr>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): ParseResult<PExpr> = when (val char = state.char) {
    null, '\n' -> missingDoubleQuoteError failAt state.lastPos
    '"' -> finishString(parts, builder, lastStart, state, startPos)
    '$' -> {
        val interState = state.next
        when (interState.char) {
            in identStartChars -> {
                val (ident, afterIdent) = handleIdent(state.next)
                handleSingleLineString(
                    afterIdent, startPos,
                    parts.interpolating(builder, lastStart, interState.pos, ident.map(::IdentExpr)),
                    StringBuilder(), afterIdent.pos
                )
            }
            '{' -> handleExprScope(state.nextActual, state.pos).flatMap { scope, afterScope ->
                handleSingleLineString(
                    afterScope, startPos,
                    parts.interpolating(builder, lastStart, interState.pos, scope.toExpr()),
                    StringBuilder(), afterScope.pos
                )
            }
            else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
        }
    }
    '\\' -> handleEscaped(state.next).flatMap { escaped, afterState ->
        handleSingleLineString(afterState, startPos, parts, builder.append(escaped), lastStart)
    }
    else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
}

tailrec fun handleMultiLineString(
    state: ParseState,
    startPos: StringPos,
    parts: List<PExpr>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): ParseResult<PExpr> = when (val char = state.char) {
    null -> unclosedMultilineStringError failAt startPos..state.pos
    '"' -> if (state.next.startWith("\"\"")) {
        finishString(parts, builder, lastStart, state + 2, startPos)
    } else handleMultiLineString(state.next, startPos, parts, builder.append('"'), lastStart)
    '$' -> {
        val interState = state.next
        when (interState.char) {
            in identStartChars -> {
                val (ident, afterIdent) = handleIdent(state.next)
                handleSingleLineString(
                    afterIdent, startPos,
                    parts.interpolating(builder, lastStart, interState.pos, ident.map(::IdentExpr)),
                    StringBuilder(), afterIdent.pos
                )
            }
            '{' -> handleExprScope(state.nextActual, state.pos).flatMap { scope, afterScope ->
                @Suppress("NON_TAIL_RECURSIVE_CALL")
                handleMultiLineString(
                    afterScope, startPos,
                    parts.interpolating(builder, lastStart, interState.pos, scope.toExpr()),
                    StringBuilder(), afterScope.pos
                )
            }
            else -> handleMultiLineString(state.next, startPos, parts, builder.append(char), lastStart)
        }
    }
    else -> handleMultiLineString(state.next, startPos, parts, builder.append(char), lastStart)
}

private fun List<PExpr>.interpolating(
    builder: StringBuilder,
    lastStart: StringPos,
    interStart: Int,
    expr: PExpr
): List<PExpr> = this + (StringExpr(builder.toString()) at lastStart..interStart) + expr

private fun finishString(
    parts: List<PExpr>,
    builder: StringBuilder,
    lastStart: StringPos,
    endState: ParseState,
    startPos: StringPos
): ParseResult.Success<Positioned<Expression>> =
    (if (parts.isEmpty()) StringExpr(builder.toString())
    else BinaryOpsExpr(
        (parts + (StringExpr(builder.toString()) at lastStart..endState.pos))
            .toBinOps(BinOpsList.Head(parts.first()), 1)
    )) at startPos..endState.pos succTo endState.next

private tailrec fun List<PExpr>.toBinOps(last: BinOpsList, index: Int): BinOpsList = if (index < size) {
    val current = get(index)
    toBinOps(last.appendSymbol("+" at current.area.first, current), index + 1)
} else last