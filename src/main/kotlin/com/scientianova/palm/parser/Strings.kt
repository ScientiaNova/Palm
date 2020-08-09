package com.scientianova.palm.parser

import com.scientianova.palm.errors.unclosedEscapeCharacterError
import com.scientianova.palm.errors.missingDoubleQuoteError
import com.scientianova.palm.errors.unclosedMultilineStringError
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at
import com.scientianova.palm.util.map

tailrec fun handleSingleLineString(
    state: ParseState,
    startPos: StringPos,
    parts: List<Pair<PExpr, PBinOp>>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): Pair<PExpr, ParseState> = when (val char = state.char) {
    null, '\n' -> missingDoubleQuoteError throwAt state.lastPos
    '"' ->
        (if (parts.isEmpty()) StringExpr(builder.toString())
        else BinaryOpsExpr(parts, StringExpr(builder.toString()) at lastStart..state.lastPos)) at
                startPos..state.lastPos to state.next
    '$' -> {
        val next = state.nextChar
        val interStart = state.nextPos
        when {
            next?.isIdentifierPart() == true -> {
                val (identifier, afterState) = handleIdentifier(
                    state.code, interStart, interStart, StringBuilder()
                )
                handleSingleLineString(
                    afterState, startPos,
                    parts +
                            ((StringExpr(builder.toString()) at lastStart..interStart) to (plus at interStart)) +
                            (identifier.map(::IdentExpr) to (plus at afterState.pos)),
                    StringBuilder(), afterState.pos
                )
            }
            next == '{' -> {
                val (scope, afterState) = handleExprScope(state.nextActual, state.pos)
                handleSingleLineString(
                    afterState, startPos,
                    parts +
                            ((StringExpr(builder.toString()) at lastStart..interStart) to (plus at interStart)) +
                            (scope.map(::ScopeExpr) to (plus at afterState.pos)),
                    StringBuilder(), afterState.pos
                )
            }
            else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
        }
    }
    '\\' -> {
        val (escaped, afterState) =
            handleEscaped(state.next) ?: unclosedEscapeCharacterError throwAt state.nextPos
        handleSingleLineString(afterState, startPos, parts, builder.append(escaped), lastStart)
    }
    else -> handleSingleLineString(state.next, startPos, parts, builder.append(char), lastStart)
}

tailrec fun handleMultiLineString(
    state: ParseState,
    startPos: StringPos,
    parts: List<Pair<PExpr, PBinOp>>,
    builder: StringBuilder,
    lastStart: StringPos = startPos
): Pair<PExpr, ParseState> = when (val char = state.char) {
    null -> unclosedMultilineStringError throwAt startPos..state.pos
    '"' -> {
        val second = state.next
        if (second.nextChar == '"') {
            val third = second + 1
            if (third.char == '"')
                BinaryOpsExpr(parts, (StringExpr(builder.toString()) at lastStart..state.lastPos)) at
                        startPos..state.lastPos to third
            else handleMultiLineString(
                third,
                startPos,
                parts,
                builder.append("\"\""),
                lastStart
            )
        } else handleMultiLineString(
            second,
            startPos,
            parts,
            builder.append('"'),
            lastStart
        )
    }
    '$' -> {
        val next = state.nextChar
        val interStart = state.nextPos
        when {
            next?.isIdentifierPart() == true -> {
                val (identifier, afterState) = handleIdentifier(
                    state.code, interStart, interStart, StringBuilder()
                )
                handleSingleLineString(
                    afterState, startPos,
                    parts +
                            ((StringExpr(builder.toString()) at lastStart..interStart) to (plus at interStart)) +
                            (identifier.map(::IdentExpr) to (plus at afterState.pos)),
                    StringBuilder(), afterState.pos
                )
            }
            next == '{' -> {
                val (scope, afterState) = handleExprScope(state.nextActual, state.pos)
                handleMultiLineString(
                    afterState, startPos,
                    parts +
                            ((StringExpr(builder.toString()) at lastStart..interStart) to (plus at interStart)) +
                            (scope.map(::ScopeExpr) to (plus at afterState.pos)),
                    StringBuilder(), afterState.pos
                )
            }
            else -> handleMultiLineString(
                state.next,
                startPos,
                parts,
                builder.append(char),
                lastStart
            )
        }
    }
    else -> handleMultiLineString(
        state.next,
        startPos,
        parts,
        builder.append(char),
        lastStart
    )
}