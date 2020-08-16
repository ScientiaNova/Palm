package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

sealed class Expression
typealias PExpr = Positioned<Expression>

data class IdentExpr(val name: String) : Expression()
data class OpRefExpr(val symbol: String) : Expression()

data class CallParams(val free: List<PExpr> = emptyList(), val named: Map<PString, PExpr> = emptyMap()) {
    operator fun plus(expr: PExpr) = CallParams(free + expr, named)
    operator fun plus(pair: Pair<PString, PExpr>) = CallParams(free, named + pair)
}

data class CallExpr(
    val expr: PExpr,
    val params: CallParams
) : Expression()

typealias LambdaParams = List<Pair<PString, PType?>>

data class LambdaExpr(
    val params: LambdaParams,
    val scope: ExprScope
) : Expression()

sealed class Condition
data class ExprCondition(val expr: PExpr) : Condition()
data class DecCondition(val pattern: PPattern, val expr: PExpr) : Condition()

data class IfExpr(
    val cond: List<Condition>,
    val ifTrue: ExprScope,
    val ifFalse: ExprScope?
) : Expression()

typealias WhenBranch = Pair<PPattern, PExpr>

data class WhenExpr(
    val comparing: PExpr,
    val branches: List<WhenBranch>
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

data class PrefixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpr) : Expression()
data class BinaryOpsExpr(val list: BinOpsList) : Expression()

sealed class BinOpsList {
    data class Head(val value: PExpr) : BinOpsList()
    data class Ident(val child: BinOpsList, val ident: PString, val value: PExpr) : BinOpsList()
    data class Symbol(val child: BinOpsList, val symbol: PString, val value: PExpr) : BinOpsList()
    data class Is(val child: BinOpsList, val type: PType) : BinOpsList()
    data class As(val child: BinOpsList, val type: PType, val handling: AsHandling) : BinOpsList()
}

fun BinOpsList.appendIdent(ident: PString, expr: PExpr) = BinOpsList.Ident(this, ident, expr)
fun BinOpsList.appendSymbol(symbol: PString, expr: PExpr) = BinOpsList.Symbol(this, symbol, expr)
fun BinOpsList.appendIs(type: PType) = BinOpsList.Is(this, type)
fun BinOpsList.appendAs(type: PType, handling: AsHandling) = BinOpsList.As(this, type, handling)
fun BinOpsList.toExpr(start: StringPos) =
    if (this is BinOpsList.Head) value
    else BinaryOpsExpr(this) at start..lastPos

inline fun BinOpsList.map(area: StringArea, fn: (PExpr) -> ParseResult<PExpr>) = when (this) {
    is BinOpsList.Head -> fn(value).map(BinOpsList::Head)
    is BinOpsList.Ident -> fn(value).map { BinOpsList.Ident(child, ident, it) }
    is BinOpsList.Symbol -> fn(value).map { BinOpsList.Symbol(child, symbol, it) }
    else -> postfixOperationOnTypeError errAt area
}

inline fun BinOpsList.map(pos: StringPos, fn: (PExpr) -> ParseResult<PExpr>) = map(pos..pos, fn)

val BinOpsList.lastPos
    get() = when (this) {
        is BinOpsList.Head -> value.area.last
        is BinOpsList.Ident -> value.area.last
        is BinOpsList.Symbol -> value.area.last
        is BinOpsList.Is -> type.area.last
        is BinOpsList.As -> type.area.last
    }

enum class AsHandling { Safe, Nullable, Unsafe }

object ContinueExpr : Expression()
data class BreakExpr(val expr: PExpr?) : Expression()
data class ReturnExpr(val expr: PExpr?) : Expression()

inline fun <T> ParseState.requireChar(char: Char, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (this.char == char) then(next) else error errAt pos

inline fun <T> ParseState.requireIdent(ident: String, error: PalmError, then: (ParseState) -> ParseResult<T>) =
    if (this.startWithIdent(ident)) then(next + ident.length) else error errAt pos

fun handleDecName(state: ParseState): ParseResult<PString> {
    val (ident, afterIdent) = handleIdent(state)
    return when (ident.value) {
        "" -> missingDeclarationNameError errAt ident.area
        in keywords -> keywordDecNameError(ident.value) errAt ident.area
        else -> ident succTo afterIdent
    }
}

fun PString.startExpr(
    afterIdent: ParseState,
    scoped: Boolean,
    excludeCurly: Boolean = scoped
) = handleIdentSubexpr(this, afterIdent, scoped).flatMap { firstExpr, afterFirst ->
    if (scoped) handleScopedBinOps(afterFirst.actual, area.first, BinOpsList.Head(firstExpr))
    else handleInlinedBinOps(afterFirst.actual, area.first, excludeCurly, BinOpsList.Head(firstExpr))
}

inline fun <T : Any> growListFromResults(
    startState: ParseState,
    fn: (List<T>, ParseState) -> ParseResult<T?>
): ParseResult<Nothing> {
    var state = startState
    var list = emptyList<T>()
    while (true) when (val res = fn(list, state)) {
        is ParseResult.Success -> {
            state = res.next
            res.value?.let { list = list + it }
        }
        is ParseResult.Failure -> return res
    }
}

fun handleExprScope(
    startState: ParseState,
    start: StringPos
): ParseResult<PExprScope> = growListFromResults(startState) { list: List<ScopeStatement>, state ->
    when (state.char) {
        null -> unclosedScopeError errAt state.pos
        '}' -> return ExprScope(list) at start..state.pos succTo state.next
        ';' -> null succTo state.nextActual
        in identStartChars -> {
            val (ident, afterIdent) = handleIdent(state)
            when (ident.value) {
                "val" -> handleDeclaration(afterIdent.actual, true)
                "var" -> handleDeclaration(afterIdent.actual, false)
                else -> ident.startExpr(afterIdent, true).map(::ExprStatement)
            }.flatMap { statement, afterState ->
                val sepState = afterState.actualOrBreak
                when (sepState.char) {
                    '\n', ';' -> statement succTo sepState.nextActual
                    '}' -> return ExprScope(list + statement) at start..state.pos succTo state.next
                    else -> missingExpressionSeparatorError errAt sepState.pos
                }
            }
        }
        else -> handleScopedExpr(state).flatMap { expr, afterExpr ->
            val statement = ExprStatement(expr)
            val sepState = afterExpr.actualOrBreak
            when (sepState.char) {
                '\n', ';' -> statement succTo sepState.nextActual
                '}' -> return ExprScope(list + statement) at start..state.pos succTo state.next
                else -> missingExpressionSeparatorError errAt sepState.pos
            }
        }
    }
}

fun handleDeclaration(state: ParseState, mutable: Boolean): ParseResult<ScopeStatement> =
    handleDecName(state).flatMap { name, afterName ->
        val symbolState = afterName.actualOrBreak
        when (symbolState.char) {
            ':' -> handleType(symbolState.nextActualOrBreak, true).flatMap { type, afterType ->
                val maybeEquals = afterType.actualOrBreak
                if (maybeEquals.startWithSymbol("=")) handleScopedExpr(maybeEquals.nextActualOrBreak).map { expr ->
                    VariableDecStatement(name, mutable, type, expr)
                } else VariableDecStatement(name, mutable, type, null) succTo afterType
            }
            '=' -> handleScopedExpr(symbolState.nextActualOrBreak).map { expr ->
                VariableDecStatement(name, mutable, null, expr)
            }
            else -> invalidVariableDeclarationError errAt afterName
        }
    }

fun expectScope(state: ParseState, error: PalmError) =
    if (state.char == '{') handleExprScope(state.nextActual, state.pos)
    else error errAt state

fun handleInlinedExpr(
    state: ParseState,
    excludeCurly: Boolean
) = handleSubexpr(state, false).flatMap { firstPart, next ->
    handleInlinedBinOps(next, state.pos, excludeCurly, BinOpsList.Head(firstPart))
}

fun handleScopedExpr(state: ParseState) = handleSubexpr(state, true).flatMap { firstPart, next ->
    handleScopedBinOps(next, state.pos, BinOpsList.Head(firstPart))
}

fun handleInlinedBinOps(
    state: ParseState,
    start: StringPos,
    excludeCurly: Boolean,
    list: BinOpsList
): ParseResult<PExpr> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (op.value.isPostfixOp(afterOp)) {
        list.map(op.area) { expr ->
            PostfixOpExpr(op, expr) at expr.area.first..op.area.last succTo afterOp
        }.flatMap { newList, next ->
            handleInlinedBinOps(next, start, excludeCurly, newList)
        }
    } else handleSubexpr(afterOp.actual, false).flatMap { sub, next ->
        handleInlinedBinOps(next, start, excludeCurly, list.appendSymbol(op, sub))
    }
} else {
    val actual = state.actual
    when (actual.char) {
        null -> finishBinOps(start, list, state)
        in identStartChars -> {
            val (infix, afterInfix) = handleIdent(actual)
            when (infix.value) {
                in keywords -> finishBinOps(start, list, state)
                "is" -> handleType(afterInfix.actual, false).flatMap { type, next ->
                    handleInlinedBinOps(next, start, excludeCurly, list.appendIs(type))
                }
                "as" -> {
                    val (handling, typeStart) = when (afterInfix.char) {
                        '!' -> AsHandling.Unsafe to afterInfix.nextActual
                        '?' -> AsHandling.Nullable to afterInfix.nextActual
                        else -> AsHandling.Safe to afterInfix.actual
                    }
                    handleType(typeStart, false).flatMap { type, next ->
                        handleInlinedBinOps(next, start, excludeCurly, list.appendAs(type, handling))
                    }
                }
                else -> handleSubexpr(afterInfix.actual, false).flatMap { part, next ->
                    handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                }
            }
        }
        '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
            handleSubexpr(afterInfix.nextActual, false).flatMap { part, next ->
                handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
            }
        }
        in symbolChars -> {
            val (op, afterOp) = handleSymbol(actual)
            val symbol = op.value
            if (afterOp.char?.isWhitespace() == false && !(symbol.endsWith('.') && symbol.length <= 2))
                invalidPrefixOperatorError errAt afterOp.pos
            else when (symbol) {
                "->" -> finishBinOps(start, list, state)
                else -> handleSubexpr(afterOp.actual, false).flatMap { part, next ->
                    handleInlinedBinOps(next.actual, start, excludeCurly, list.appendSymbol(op, part))
                }
            }
        }
        '(' -> list.map(state.pos) { expr -> handleCall(state.nextActual, expr, excludeCurly) }
            .flatMap { newList, next ->
                handleInlinedBinOps(next, start, excludeCurly, newList)
            }
        '{' -> if (excludeCurly) {
            finishBinOps(start, list, state)
        } else list.map(state.pos) { expr ->
            handleLambda(state.nextActual, state.pos).map { lambda ->
                CallExpr(expr, CallParams(listOf(lambda))) at expr.area.first..lambda.area.first
            }
        }.flatMap { nextList, next -> handleInlinedBinOps(next, start, excludeCurly, nextList) }
        else -> finishBinOps(start, list, state)
    }
}

fun handleScopedBinOps(
    state: ParseState,
    start: StringPos,
    list: BinOpsList
): ParseResult<PExpr> = if (state.char?.isSymbolPart() == true) {
    val (op, afterOp) = handleSymbol(state)
    if (op.value.isPostfixOp(afterOp)) {
        list.map(op.area) { expr ->
            PostfixOpExpr(op, expr) at expr.area.first..op.area.last succTo afterOp
        }.flatMap { newList, next ->
            handleScopedBinOps(next, start, newList)
        }
    } else handleSubexpr(afterOp.actual, true).flatMap { sub, next ->
        handleScopedBinOps(next, start, list.appendSymbol(op, sub))
    }
} else {
    val actual = state.actualOrBreak
    when (actual.char) {
        null -> finishBinOps(start, list, state)
        in identStartChars -> {
            val (infix, afterInfix) = handleIdent(actual)
            when (infix.value) {
                in keywords -> finishBinOps(start, list, state)
                "is" -> handleType(afterInfix.actual, false).flatMap { type, next ->
                    handleScopedBinOps(next, start, list.appendIs(type))
                }
                "as" -> {
                    val (handling, typeStart) = when (afterInfix.char) {
                        '!' -> AsHandling.Unsafe to afterInfix.nextActual
                        '?' -> AsHandling.Nullable to afterInfix.nextActual
                        else -> AsHandling.Safe to afterInfix.actual
                    }
                    handleType(typeStart, false).flatMap { type, next ->
                        handleScopedBinOps(next, start, list.appendAs(type, handling))
                    }
                }
                else -> handleSubexpr(afterInfix.nextActual, true).flatMap { part, next ->
                    handleScopedBinOps(next, start, list.appendIdent(infix, part))
                }
            }
        }
        '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
            handleSubexpr(afterInfix.nextActual, true).flatMap { part, next ->
                handleScopedBinOps(next, start, list.appendIdent(infix, part))
            }
        }
        in symbolChars -> {
            val (op, afterOp) = handleSymbol(actual)
            val symbol = op.value
            if (afterOp.char?.isWhitespace() == false && !(symbol.endsWith('.') && symbol.length <= 2))
                invalidPrefixOperatorError errAt afterOp.pos
            else when (symbol) {
                "->" -> finishBinOps(start, list, state)
                else -> handleSubexpr(afterOp.actual, true).flatMap { part, next ->
                    handleScopedBinOps(next.actual, start, list.appendSymbol(op, part))
                }
            }
        }
        '(' -> list.map(state.pos) { expr -> handleCall(state.nextActual, expr, false) }
            .flatMap { newList, next -> handleScopedBinOps(next, start, newList) }
        else -> {
            val maybeCurly = actual.actual
            if (maybeCurly.char == '{') list.map(state.pos) { expr ->
                handleLambda(state.nextActual, state.pos).map { lambda ->
                    CallExpr(expr, CallParams(listOf(lambda))) at expr.area.first..lambda.area.first
                }
            }.flatMap { nextList, next -> handleScopedBinOps(next, start, nextList) }
            else finishBinOps(start, list, state)
        }
    }
}

private fun String.isPostfixOp(afterOp: ParseState) =
    !(endsWith('.') && length <= 2) && afterOp.char.isAfterPostfix()

private fun finishBinOps(
    start: StringPos,
    list: BinOpsList,
    state: ParseState
) = list.toExpr(start) succTo state

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
    '{' -> handleLambda(state.nextActual, state.pos)
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
    "if" -> handleIf(next.actual, ident.area.first)
    "for" -> handleFor(next.actual, ident.area.first)
    "when" -> handleWhen(next.actual, ident.area.first)
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
): ParseResult<PExpr> =
    (if (scoped) handleScopedExpr(next.actual) else handleInlinedExpr(next.actual, false))
        .biFlatMap({ expr, afterState -> fn(expr) at normalArea.first..expr.area.last succTo afterState })
        { fn(null) at normalArea succTo next }

fun handleCall(
    state: ParseState,
    on: PExpr,
    excludeCurly: Boolean
) = handleCallParams(state, on.area.first, CallParams()).flatMap { params, afterParams ->
    val actual = afterParams.actual
    if (!excludeCurly && actual.char == '{') handleLambda(actual.nextActual, actual.pos).map { lambda ->
        CallExpr(on, params.value + lambda) at on.area.first..lambda.area.last
    } else CallExpr(on, params.value) at params.area succTo afterParams
}

fun handleCallParams(
    state: ParseState,
    start: StringPos,
    params: CallParams
): ParseResult<Positioned<CallParams>> = if (state.char == ')') {
    params at start..state.pos succTo state.next
} else {
    if (state.char?.isIdentifierStart() == true) {
        val (ident, afterIdent) = handleIdent(state)
        val eqState = afterIdent.actual
        if (eqState.startWithSymbol("=")) {
            handleInlinedExpr(eqState.nextActual, false).map { expr ->
                params + (ident to expr)
            }
        } else ident.startExpr(afterIdent, false).map(params::plus)
    } else {
        handleInlinedExpr(state, false).map(params::plus)
    }.flatMap { newParams, afterExpr ->
        val symbolState = afterExpr.actual
        when (symbolState.char) {
            ')' -> newParams at start..symbolState.pos succTo symbolState.next
            ',' -> handleCallParams(symbolState.nextActual, start, newParams)
            else -> unclosedParenthesisError errAt symbolState.pos
        }
    }
}

fun handleParenthesizedExpr(
    state: ParseState
): ParseResult<PExpr> = if (state.char == ')') {
    emptyParenthesesOnExprError errAt state.pos
} else handleInlinedExpr(state, false).expectActual(')', unclosedParenthesisError)

fun handleList(
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
    when (symbolState.char) {
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

fun handleIf(
    state: ParseState,
    start: StringPos
) = handleConditions(state).flatMap { conditions, afterConditions ->
    expectScope(afterConditions.actual, missingScopeAfterIfError).zipWithNext({ i, e: Positioned<ExprScope?> ->
        IfExpr(conditions, i.value, e.value) at start..e.area.last
    }) { afterIfScope ->
        val elseScope = afterIfScope.actual
        if (elseScope.startWithIdent("else")) {
            expectScope((elseScope + 4).actual, missingScopeAfterElseError)
        } else null at afterIfScope.lastPos succTo afterIfScope
    }
}

fun handleConditions(
    startState: ParseState
): ParseResult<List<Condition>> = growListFromResults(startState) { list: List<Condition>, state ->
    if (state.char?.isIdentifierStart() == true) {
        val (ident, afterIdent) = handleIdent(state)
        when (ident.value) {
            "val" -> handlePatternCond(afterIdent, DeclarationType.VAL)
            "var" -> handlePatternCond(afterIdent, DeclarationType.VAR)
            else -> ident.startExpr(afterIdent, scoped = false, excludeCurly = true).map(::ExprCondition)
        }
    } else {
        handleInlinedExpr(state, true).map { expr -> ExprCondition(expr) }
    }.flatMap { cond, afterCond ->
        val maybeComma = afterCond.actual
        if (maybeComma.char == ',') cond succTo maybeComma.nextActual
        else return (list + cond) succTo maybeComma
    }
}

private fun handlePatternCond(
    afterIdent: ParseState,
    decType: DeclarationType
) = handlePattern(afterIdent.actual, decType, true).flatMapIfActualSymbol("=") { pattern, afterEqState ->
    handleInlinedExpr(afterEqState.actual, true).map { expr -> DecCondition(pattern, expr) }
}

fun handleFor(state: ParseState, start: StringPos) = handleDecName(state).flatMap { name, afterName ->
    afterName.actual.requireIdent("in", missingInInForError) { afterIn ->
        handleInlinedExpr(afterIn.actual, true).flatMap { iterable, afterIter ->
            expectScope(afterIter, missingScopeAfterForError).map { scope ->
                ForExpr(name, iterable, scope.value) at start..scope.area.last
            }
        }
    }
}

fun handleWhen(state: ParseState, start: StringPos): ParseResult<PExpr> = if (state.char == '{') {
    handleWhenBranches(state.nextActual, trueExpr at state.pos, state.pos)
} else handleInlinedExpr(state, true).flatMapIfActual('{', missingScopeAfterWhenError) { comparing, afterCurly ->
    handleWhenBranches(afterCurly.actual, comparing, start)
}

private fun handleWhenBranches(
    startState: ParseState,
    comparing: PExpr,
    start: StringPos
): ParseResult<PExpr> = growListFromResults(startState) { list: List<WhenBranch>, state ->
    when (state.char) {
        null -> unclosedWhenError errAt state
        '}' -> return WhenExpr(comparing, list) at start..state.pos succTo state.next
        ';' -> null succTo state.nextActual
        else -> handlePattern(state, DeclarationType.NONE, false)
            .flatMapIfActualSymbol("->") { pattern, afterArrow ->
                val maybeCurly = afterArrow.actual
                (if (maybeCurly.char == '{') {
                    handleExprScope(maybeCurly.nextActual, maybeCurly.pos).toExpr()
                } else handleScopedExpr(maybeCurly)).flatMap { expr, afterExpr ->
                    val sepState = afterExpr.actual
                    when (sepState.char) {
                        '}' -> return WhenExpr(comparing, list + (pattern to expr)) at
                                start..sepState.pos succTo sepState.next
                        '\n', ';' -> (pattern to expr) succTo sepState.nextActual
                        else -> unclosedWhenError errAt sepState
                    }
                }
            }
    }
}

fun handleLambda(state: ParseState, start: StringPos): ParseResult<PExpr> {
    val (params, next) = handleLambdaParams(state, emptyList()).orDefault(state, emptyList())
    return handleExprScope(next.actual, start).map {
        it.map { scope -> LambdaExpr(params, scope) }
    }
}

private fun handleLambdaParams(
    state: ParseState,
    params: LambdaParams
): ParseResult<LambdaParams> = if (state.startWithSymbol("->")) {
    if (params.isEmpty()) invalidLambdaArgumentsError errAt state
    else params succTo state + 2
} else handleDecName(state).flatMap { name, afterName ->
    val sepState = afterName.actual
    when (sepState.char) {
        ':' -> handleType(sepState.nextActual, false).flatMap { type, afterType ->
            val sepState2 = afterType.actual
            when {
                sepState.char == ',' -> handleLambdaParams(
                    sepState2.nextActual, params + (name to type)
                )
                sepState2.startWithSymbol("->") ->
                    params + (name to type) succTo sepState2 + 2
                else -> invalidLambdaArgumentsError errAt sepState2
            }
        }
        ',' -> handleLambdaParams(sepState.nextActual, params + (name to null))
        else -> if (sepState.startWithSymbol("->")) {
            params + (name to null) succTo sepState + 2
        } else invalidLambdaArgumentsError errAt sepState
    }
}