@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingDoubleQuoteError
import com.scientianova.palm.errors.missingStringError
import com.scientianova.palm.errors.unclosedMultilineStringError
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

fun <R> interpolatedExpr() = interpolatedExpr as Parser<R, PExpr>

private val interpolatedExpr: Parser<Any, PExpr> = oneOf(
    identifier<Any>().map { if (it == "this") ThisExpr else IdentExpr(it) },
    scopeExpr()
).withPos()

fun <R> string() = string as Parser<R, Expression>

private val string: Parser<Any, Expression> =
    tryChar<Any>('\"', missingStringError).takeR(
        oneOf(
            tryChar<Any>('\"').takeR(tryChar<Any>('\"').takeR { state, succ: SuccFn<Any, Expression>, cErr, _ ->
                handleMultiLineString(state, emptyList(), StringBuilder(), state.pos, succ, cErr)
            }.orDefault(emptyString)), { state, succ, cErr, _ ->
                handleSingleLineString(state, emptyList(), StringBuilder(), state.pos, succ, cErr)
            }
        )
    )

tailrec fun <R> handleSingleLineString(
    state: ParseState,
    parts: List<PExpr>,
    builder: StringBuilder,
    lastStart: StringPos,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = state.char) {
    null, '\n' -> errFn(missingDoubleQuoteError, state.area)
    '"' -> succFn(finishString(parts, builder, lastStart, state), state.next)
    '$' -> {
        val interState = state.next
        when (val res = returnResultT(interState, interpolatedExpr())) {
            is ParseResultT.Success ->
                handleSingleLineString(
                    res.next, parts.interpolating(builder, lastStart, state.nextPos,
                        res.value), StringBuilder(), res.next.pos, succFn, errFn
                )
            is ParseResultT.Error -> errFn(res.error, res.area)
            is ParseResultT.Failure ->
                handleSingleLineString(interState, parts, builder.append(char), lastStart, succFn, errFn)
        }
    }
    '\\' -> when (val res = handleEscaped(state.next)) {
        is ParseResult.Success ->
            handleSingleLineString(res.next, parts, builder.append(res.value), lastStart, succFn, errFn)
        is ParseResult.Error ->
            errFn(res.error, res.area)
    }
    else -> handleSingleLineString(state.next, parts, builder.append(char), lastStart, succFn, errFn)
}

tailrec fun <R> handleMultiLineString(
    state: ParseState,
    parts: List<PExpr>,
    builder: StringBuilder,
    lastStart: StringPos,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = when (val char = state.char) {
    null -> errFn(unclosedMultilineStringError, state.area)
    '"' -> if (state.next.startsWith("\"\"")) {
        succFn(finishString(parts, builder, lastStart, state + 2), state + 3)
    } else handleMultiLineString(state.next, parts, builder.append('"'), lastStart, succFn, errFn)
    '$' -> {
        val interState = state.next
        when (val res = returnResultT(interState, interpolatedExpr())) {
            is ParseResultT.Success ->
                handleMultiLineString(
                    res.next, parts.interpolating(builder, lastStart, state.nextPos,
                        res.value), StringBuilder(), res.next.pos, succFn, errFn
                )
            is ParseResultT.Error -> errFn(res.error, res.area)
            is ParseResultT.Failure ->
                handleMultiLineString(interState, parts, builder.append(char), lastStart, succFn, errFn)
        }
    }
    else -> handleMultiLineString(state.next, parts, builder.append(char), lastStart, succFn, errFn)
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
    endState: ParseState
): Expression =
    if (parts.isEmpty()) {
        StringExpr(builder.toString())
    } else
        BinaryOpsExpr(
            (parts + (StringExpr(builder.toString()) at lastStart..endState.pos))
                .toBinOps(BinOpsList.Head(parts.first()), 1)
        )

private tailrec fun List<PExpr>.toBinOps(last: BinOpsList, index: Int): BinOpsList = if (index < size) {
    val current = get(index)
    toBinOps(last.appendSymbol("+" at current.area.first, current), index + 1)
} else last