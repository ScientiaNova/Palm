@file:Suppress("UNCHECKED_CAST")

package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.util.*

sealed class Expression
typealias PExpr = Positioned<Expression>

data class IdentExpr(val name: String) : Expression()
data class OpRefExpr(val symbol: String) : Expression()

sealed class Arg {
    data class Free(val value: PExpr) : Arg()
    data class Named(val name: PString, val value: PExpr) : Arg()
}

data class CallArgs(val args: List<Arg> = emptyList(), val last: PExpr? = null) {
    operator fun plus(arg: Arg) = CallArgs(args + arg, last)
    fun withLast(expr: PExpr) = CallArgs(args, expr)
}

data class CallExpr(
    val expr: PExpr,
    val args: CallArgs
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

val emptyString = StringExpr("")

object NullExpr : Expression()
object ThisExpr : Expression()

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
fun BinOpsList.toExpr() =
    if (this is BinOpsList.Head) value.value
    else BinaryOpsExpr(this)

inline fun <R> BinOpsList.map(crossinline fn: (PExpr) -> PExpr): Parser<R, BinOpsList> = { state, succ, cErr, _ ->
    when (this) {
        is BinOpsList.Head -> succ(BinOpsList.Head(fn(value)), state)
        is BinOpsList.Ident -> succ(BinOpsList.Ident(child, ident, fn(value)), state)
        is BinOpsList.Symbol -> succ(BinOpsList.Symbol(child, symbol, fn(value)), state)
        is BinOpsList.Is -> cErr(postfixOperationOnTypeError, type.area.last + 1 until state.pos)
        is BinOpsList.As -> cErr(postfixOperationOnTypeError, type.area.last + 1 until state.pos)
    }
}

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

fun handleDecName(state: ParseState): ParseResult<PString> =
    expectIdent(state).errIf(keywords::contains, ::keywordDecNameError)

fun PString.startExpr(
    afterIdent: ParseState,
    scoped: Boolean,
    excludeCurly: Boolean = scoped
) = handleIdentSubexpr(this, afterIdent, scoped).flatMap { firstExpr, afterFirst ->
    if (scoped) handleScopedBinOps(afterFirst, firstExpr)
    else handleInlinedBinOps(afterFirst, firstExpr, excludeCurly)
}

fun <R> scope() = scope as Parser<R, ExprScope>

private val scope: Parser<Any, ExprScope> = { state, succ, cErr, _ ->
    if (state.char != '}') cErr(missingScopeError, state.area)
    else handleScopeBody(state.nextActual, succ, cErr)
}

fun <R> scopeExpr() = scopeExpr as Parser<R, ScopeExpr>

private val scopeExpr: Parser<Any, ScopeExpr> =
    matchChar<Any>('{', missingScopeError).takeR { state, succ: SuccFn<Any, ExprScope>, cErr, _ ->
        handleScopeBody(state.actual, succ, cErr)
    }.map(::ScopeExpr)

fun <R> handleScopeBody(
    startState: ParseState,
    succFn: SuccFn<R, ExprScope>,
    errFn: ErrFn<R>
): R = loopValue(emptyList<ScopeStatement>() to startState) { (list, state) ->
    when (state.char) {
        null -> return errFn(unclosedScopeError, state.area)
        '}' -> return succFn(ExprScope(list), state.next)
        ';' -> list to state.nextActual
        in identStartChars -> {
            val (ident, afterIdent) = handleIdent(state)
            when (ident.value) {
                "val" -> handleDeclaration(afterIdent.actual, true)
                "var" -> handleDeclaration(afterIdent.actual, false)
                else -> ident.startExpr(afterIdent, true).map(::ExprStatement)
            }.flatMap { statement, afterState ->
                val sepState = afterState.actualOrBreak
                when (sepState.char) {
                    '\n', ';' -> list + statement succTo sepState.nextActual
                    '}' -> return ExprScope(list + statement) at start..sepState.pos succTo sepState.next
                    else -> missingExpressionSeparatorError errAt sepState.pos
                }
            }
        }
        else -> handleScopedExpr(state).flatMap { expr, afterExpr ->
            val statement = ExprStatement(expr)
            val sepState = afterExpr.actualOrBreak
            when (sepState.char) {
                '\n', ';' -> list + statement succTo sepState.nextActual
                '}' -> return ExprScope(list + statement) succTo sepState.next
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
                    VarDecStatement(name, mutable, type, expr)
                } else VarDecStatement(name, mutable, type, null) succTo afterType
            }
            '=' -> handleScopedExpr(symbolState.nextActualOrBreak).map { expr ->
                VarDecStatement(name, mutable, null, expr)
            }
            else -> invalidVariableDeclarationError errAt afterName
        }
    }

private val asHandling: Parser<Any, AsHandling> = { state, succ, _, _ ->
    when (state.char) {
        '!' -> succ(AsHandling.Unsafe, state.next)
        '?' -> succ(AsHandling.Nullable, state.next)
        else -> succ(AsHandling.Safe, state.next)
    }
}

fun <R> asHandling() = asHandling as Parser<R, AsHandling>

fun expectScope(state: ParseState, error: PalmError) =
    if (state.char == '{') handleScopeBody(state.nextActual, state.pos)
    else error errAt state

fun handleInlinedExpr(
    state: ParseState,
    excludeCurly: Boolean
) = handleSubexpr(state, false).flatMap { firstPart, next ->
    handleInlinedBinOps(next, firstPart, excludeCurly)
}

fun handleScopedExpr(state: ParseState) = handleSubexpr(state, true).flatMap { firstPart, next ->
    handleScopedBinOps(next, firstPart)
}

private val directOp: Parser<Any, (BinOpsList) -> BinOpsList> = symbol<Any>().withPos().flatMap { op ->
    val symbol = op.value
    if (symbol.length == 2 && symbol.last() == '.') whitespace<Any>().takeR(subExpr()).withPos().map { expr ->
        { list: BinOpsList -> list.appendSymbol(op, expr) }
    } else oneOf(
        subExpr<Any>().withPos().map { expr ->
            { list: BinOpsList -> list.appendSymbol(op, expr) }
        }, valueP { list: BinOpsList -> TODO() }
    )
}

fun handleInlinedBinOps(
    startState: ParseState,
    first: PExpr,
    excludeCurly: Boolean,
): ParseResult<PExpr> = reuseWhileSuccess(startState, BinOpsList.Head(first)) { list: BinOpsList, state ->
    if (state.char?.isSymbolPart() == true) {
        val (op, afterOp) = handleSymbol(state)
        if (op.value.isPostfixOp(afterOp)) {
            list.map(op.area) { expr ->
                PostfixOpExpr(op, expr) at expr.area.first..op.area.last succTo afterOp
            }
        } else handleSubexpr(afterOp.actual, false).map { sub ->
            list.appendSymbol(op, sub)
        }
    } else {
        val actual = state.actual
        when (actual.char) {
            in identStartChars -> {
                val (infix, afterInfix) = handleIdent(actual)
                when (infix.value) {
                    in keywords -> return finishBinOps(first.area.first, list, state)
                    "is" -> handleType(afterInfix.actual, false).map(list::appendIs)
                    "as" -> {
                        val (handling, typeStart) = when (afterInfix.char) {
                            '!' -> AsHandling.Unsafe to afterInfix.nextActual
                            '?' -> AsHandling.Nullable to afterInfix.nextActual
                            else -> AsHandling.Safe to afterInfix.actual
                        }
                        handleType(typeStart, false).map { type ->
                            list.appendAs(type, handling)
                        }
                    }
                    else -> handleSubexpr(afterInfix.actual, false).map { part ->
                        list.appendIdent(infix, part)
                    }
                }
            }
            '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
                handleSubexpr(afterInfix.nextActual, false).map { part ->
                    list.appendIdent(infix, part)
                }
            }
            in symbolChars -> {
                val (op, afterOp) = handleSymbol(actual)
                val symbol = op.value
                if (afterOp.char?.isWhitespace() == false && !(symbol.endsWith('.') && symbol.length <= 2))
                    invalidPrefixOperatorError errAt afterOp.pos
                else when (symbol) {
                    "->" -> return finishBinOps(first.area.first, list, state)
                    else -> handleSubexpr(afterOp.actual, false).map { part ->
                        list.appendSymbol(op, part)
                    }
                }
            }
            '(' -> list.map(actual.pos) { expr -> handleCall(actual.nextActual, expr, excludeCurly) }
            '{' -> if (excludeCurly) {
                return finishBinOps(first.area.first, list, state)
            } else list.map(actual.pos) { expr ->
                handleLambda(actual.nextActual, actual.pos).map { lambda ->
                    CallExpr(expr, CallArgs(last = lambda)) at expr.area.first..lambda.area.first
                }
            }
            else -> return finishBinOps(first.area.first, list, state)
        }
    }
}

fun handleScopedBinOps(
    startState: ParseState,
    first: PExpr
): ParseResult<PExpr> = reuseWhileSuccess(startState, BinOpsList.Head(first)) { list: BinOpsList, state ->
    if (state.char?.isSymbolPart() == true) {
        val (op, afterOp) = handleSymbol(state)
        if (op.value.isPostfixOp(afterOp)) {
            list.map(op.area) { expr ->
                PostfixOpExpr(op, expr) at expr.area.first..op.area.last succTo afterOp
            }
        } else handleSubexpr(afterOp.actual, true).map { sub ->
            list.appendSymbol(op, sub)
        }
    } else {
        val actual = state.actualOrBreak
        when (actual.char) {
            in identStartChars -> {
                val (infix, afterInfix) = handleIdent(actual)
                when (infix.value) {
                    in keywords -> return finishBinOps(first.area.first, list, state)
                    "is" -> handleType(afterInfix.actual, false).map(list::appendIs)
                    "as" -> {
                        val (handling, typeStart) = when (afterInfix.char) {
                            '!' -> AsHandling.Unsafe to afterInfix.nextActual
                            '?' -> AsHandling.Nullable to afterInfix.nextActual
                            else -> AsHandling.Safe to afterInfix.actual
                        }
                        handleType(typeStart, false).map { type ->
                            list.appendAs(type, handling)
                        }
                    }
                    else -> handleSubexpr(afterInfix.nextActual, true).map { part ->
                        list.appendIdent(infix, part)
                    }
                }
            }
            '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
                handleSubexpr(afterInfix.nextActual, true).map { part ->
                    list.appendIdent(infix, part)
                }
            }
            in symbolChars -> {
                val (op, afterOp) = handleSymbol(actual)
                val symbol = op.value
                if (afterOp.char?.isWhitespace() == false && !(symbol.endsWith('.') && symbol.length <= 2))
                    invalidPrefixOperatorError errAt afterOp.pos
                else when (symbol) {
                    "->" -> return finishBinOps(first.area.first, list, state)
                    else -> handleSubexpr(afterOp.actual, true).map { part ->
                        list.appendSymbol(op, part)
                    }
                }
            }
            '(' -> list.map(actual.pos) { expr -> handleCall(actual.nextActual, expr, false) }
            else -> {
                val maybeCurly = actual.actual
                if (maybeCurly.char == '{') list.map(maybeCurly.pos) { expr ->
                    handleLambda(maybeCurly.nextActual, maybeCurly.pos).map { lambda ->
                        CallExpr(expr, CallArgs(last = lambda)) at expr.area.first..lambda.area.first
                    }
                } else return finishBinOps(first.area.first, list, state)
            }
        }
    }
}

private fun String.isPostfixOp(afterOp: ParseState) =
    !(endsWith('.') && length <= 2) && afterOp.char.isAfterPostfix()

private fun finishBinOps(
    start: StringPos,
    list: BinOpsList,
    state: ParseState
) = list.toExpr() succTo state

fun <R> subExpr() = subExpr as Parser<R, Expression>

private val subExpr: Parser<Any, Expression> by lazy {
    oneOfOrError(
        missingExpressionError,
        number(),
        char(),
        string(),
        tickedIdentifier<Any>().map(::IdentExpr),
        normalIdentifier<Any>().flatMap(::identSubExpr)
    )
}

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
        else handleSubexpr(exprState, scoped).map { expr ->
            PrefixOpExpr(symbol, expr) at state.pos..expr.area.last
        }
    }
    else -> invalidExpressionError errAt state.pos
}

fun <R> trueConst() = trueConst as Parser<R, Expression>
private val trueConst: Parser<Any, Expression> = valueP(trueExpr)

fun <R> falseConst() = falseConst as Parser<R, Expression>
private val falseConst: Parser<Any, Expression> = valueP(falseExpr)

fun <R> nullConst() = nullConst as Parser<R, Expression>
private val nullConst: Parser<Any, Expression> = valueP(NullExpr)

fun <R> continueConst() = continueConst as Parser<R, Expression>
private val continueConst: Parser<Any, Expression> = valueP(ContinueExpr)

fun <R> thisConst() = thisConst as Parser<R, Expression>
private val thisConst: Parser<Any, Expression> = valueP(ThisExpr)

fun <R> identSubExpr(ident: String): Parser<R, Expression> = when (ident) {
    "true" -> trueConst()
    "false" -> falseConst()
    "null" -> nullConst()
    "this" -> thisConst()
    "continue" -> continueConst()
    else -> valueP(IdentExpr(ident))
}

fun handleIdentSubexpr(
    ident: PString,
    next: ParseState,
    scoped: Boolean
): ParseResult<Positioned<Expression>> = when (ident.value) {
    "if" -> handleIf(next.actual, ident.area.first)
    "for" -> handleFor(next.actual, ident.area.first)
    "when" -> handleWhen(next.actual, ident.area.first)
    "true" -> trueCons at ident.area succTo next
    "false" -> falseExpr at ident.area succTo next
    "null" -> NullExpr at ident.area succTo next
    "this" -> ThisExpr at ident.area succTo next
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
) = handleCallArgs(state, on.area.first).flatMap { args, afterParams ->
    val actual = afterParams.actual
    if (!excludeCurly && actual.char == '{') handleLambda(actual.nextActual, actual.pos).map { lambda ->
        CallExpr(on, args.value.withLast(lambda)) at on.area.first..lambda.area.last
    } else CallExpr(on, args.value) at args.area succTo afterParams
}

fun handleCallArgs(
    startState: ParseState,
    start: StringPos
): ParseResult<Positioned<CallArgs>> = reuseWhileSuccess(startState, CallArgs()) { args, state ->
    if (state.char == ')') {
        return args at start..state.pos succTo state.next
    } else {
        if (state.char?.isIdentifierStart() == true) {
            val (ident, afterIdent) = handleIdent(state)
            val eqState = afterIdent.actual
            if (eqState.startWithSymbol("=")) {
                handleInlinedExpr(eqState.nextActual, false).map { expr ->
                    args + Arg.Named(ident, expr)
                }
            } else ident.startExpr(afterIdent, false).map { args + Arg.Free(it) }
        } else {
            handleInlinedExpr(state, false).map { args + Arg.Free(it) }
        }.flatMap { newParams, afterExpr ->
            val symbolState = afterExpr.actual
            when (symbolState.char) {
                ')' -> return newParams at start..symbolState.pos succTo symbolState.next
                ',' -> newParams succTo symbolState.nextActual
                else -> unclosedParenthesisError errAt symbolState.pos
            }
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
): ParseResult<List<Condition>> = reuseWhileSuccess(startState, emptyList<Condition>()) { list, state ->
    if (state.char?.isIdentifierStart() == true) {
        val (ident, afterIdent) = handleIdent(state)
        when (ident.value) {
            "val" -> handlePatternCond(afterIdent, DeclarationType.VAL)
            "var" -> handlePatternCond(afterIdent, DeclarationType.VAR)
            else -> ident.startExpr(afterIdent, scoped = false, excludeCurly = true).map(::ExprCondition)
        }
    } else {
        handleInlinedExpr(state, true).map(::ExprCondition)
    }.flatMap { cond, afterCond ->
        val maybeComma = afterCond.actual
        if (maybeComma.char == ',') list + cond succTo maybeComma.nextActual
        else return list + cond succTo maybeComma
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
    handleWhenBranches(state.nextActual, trueCons at state.pos, state.pos)
} else handleInlinedExpr(state, true).flatMapIfActual('{', missingScopeAfterWhenError) { comparing, afterCurly ->
    handleWhenBranches(afterCurly.actual, comparing, start)
}

private fun handleWhenBranches(
    startState: ParseState,
    comparing: PExpr,
    start: StringPos
): ParseResult<PExpr> = reuseWhileSuccess(startState, emptyList<WhenBranch>()) { list, state ->
    when (state.char) {
        null -> unclosedWhenError errAt state
        '}' -> return WhenExpr(comparing, list) at start..state.pos succTo state.next
        ';' -> list succTo state.nextActual
        else -> handlePattern(state, DeclarationType.NONE, false)
            .flatMapIfActualSymbol("->") { pattern, afterArrow ->
                val maybeCurly = afterArrow.actual
                (if (maybeCurly.char == '{') {
                    handleScopeBody(maybeCurly.nextActual, maybeCurly.pos).toExpr()
                } else handleScopedExpr(maybeCurly)).flatMap { expr, afterExpr ->
                    val sepState = afterExpr.actualOrBreak
                    when (sepState.char) {
                        '}' -> return WhenExpr(comparing, list + (pattern to expr)) at
                                start..sepState.pos succTo sepState.next
                        '\n', ';' -> list + (pattern to expr) succTo sepState.nextActual
                        else -> unclosedWhenError errAt sepState
                    }
                }
            }
    }
}

fun handleLambda(state: ParseState, start: StringPos): ParseResult<PExpr> {
    val (params, next) = handleLambdaParams(state, emptyList()).orDefault(state, emptyList())
    return handleScopeBody(next.actual, start).map {
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
}