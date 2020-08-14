package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingDoubleQuoteError
import com.scientianova.palm.errors.unclosedMultilineStringError
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map

tailrec fun handleSingleLineString(
    state: ParseState,
    startPos: StringPos,
    parts: OpenOpsList,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): ParseResult<PExpr> = when (val char = state.char) {
    null, '\n' -> missingDoubleQuoteError errAt state.lastPos
    '"' -> finishString(parts, builder, lastStart, state, startPos)
    '$' -> {
        val interState = state.next
        when (interState.char) {
            in identChars -> {
                val (ident, afterIdent) = handleIdent(state.next)
                handleSingleLineString(
                    afterIdent, startPos,
                    interpolating(parts, builder, lastStart, interState.pos, ident.map(::IdentExpr), afterIdent),
                    StringBuilder(), afterIdent.pos
                )
            }
            '{' -> handleExprScope(state.nextActual, state.pos).flatMap { scope, afterScope ->
                return handleSingleLineString(
                    afterScope, startPos,
                    interpolating(parts, builder, lastStart, interState.pos, scope.toExpr(), afterScope),
                    StringBuilder(), afterScope.pos
                )
            }
            else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
        }
    }
    '\\' -> handleEscaped(state.next).flatMap { escaped, afterState ->
        return handleSingleLineString(afterState, startPos, parts, builder.append(escaped), lastStart)
    }
    else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
}

tailrec fun handleMultiLineString(
    state: ParseState,
    startPos: StringPos,
    parts: OpenOpsList,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): ParseResult<PExpr> = when (val char = state.char) {
    null -> unclosedMultilineStringError errAt startPos..state.pos
    '"' -> if (state.next.startWith("\"\"")) {
        finishString(parts, builder, lastStart, state + 2, startPos)
    } else handleMultiLineString(state.next, startPos, parts, builder.append('"'), lastStart)
    '$' -> {
        val interState = state.next
        when (interState.char) {
            in identChars -> {
                val (ident, afterIdent) = handleIdent(state.next)
                handleSingleLineString(
                    afterIdent, startPos,
                    interpolating(parts, builder, lastStart, interState.pos, ident.map(::IdentExpr), afterIdent),
                    StringBuilder(), afterIdent.pos
                )
            }
            '{' -> handleExprScope(state.nextActual, state.pos).flatMap { scope, afterScope ->
                return handleMultiLineString(
                    afterScope, startPos,
                    interpolating(parts, builder, lastStart, interState.pos, scope.toExpr(), afterScope),
                    StringBuilder(), afterScope.pos
                )
            }
            else -> handleMultiLineString(state.next, startPos, parts, builder.append(char), lastStart)
        }
    }
    else -> handleMultiLineString(state.next, startPos, parts, builder.append(char), lastStart)
}

private fun interpolating(
    parts: OpenOpsList,
    builder: StringBuilder,
    lastStart: StringPos,
    interStart: Int,
    expr: PExpr,
    afterState: ParseState
): OpenOpsList =
    parts + ((StringExpr(builder.toString()) at lastStart..interStart) to (plus at interStart)) +
            (expr to (plus at afterState.pos))

private fun finishString(
    parts: OpenOpsList,
    builder: StringBuilder,
    lastStart: StringPos,
    endState: ParseState,
    startPos: StringPos
): ParseResult.Success<Positioned<Expression>> =
    (if (parts.isEmpty()) StringExpr(builder.toString())
    else BinaryOpsExpr(parts, (StringExpr(builder.toString()) at lastStart..endState.pos))) at
            startPos..endState.pos succTo endState.next