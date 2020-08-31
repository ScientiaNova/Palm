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

data class CallArgs(val args: List<Arg> = emptyList(), val last: PExpr? = null)

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
    val comparing: PExpr?,
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
    data class Error(val error: PalmError, val area: StringArea) : BinOpsList()
}

fun BinOpsList.appendIdent(ident: PString, expr: PExpr) = BinOpsList.Ident(this, ident, expr)
fun BinOpsList.appendSymbol(symbol: PString, expr: PExpr) = BinOpsList.Symbol(this, symbol, expr)
fun BinOpsList.appendIs(type: PType) = BinOpsList.Is(this, type)
fun BinOpsList.appendAs(type: PType, handling: AsHandling) = BinOpsList.As(this, type, handling)
fun BinOpsList.toExpr(area: StringArea) =
    if (this is BinOpsList.Head) value
    else BinaryOpsExpr(this) at area

inline fun BinOpsList.map(fn: (PExpr) -> PExpr) = when (this) {
    is BinOpsList.Head -> BinOpsList.Head(fn(value))
    is BinOpsList.Ident -> BinOpsList.Ident(child, ident, fn(value))
    is BinOpsList.Symbol -> BinOpsList.Symbol(child, symbol, fn(value))
    is BinOpsList.Is -> BinOpsList.Error(postfixOperationOnTypeError, (type.area.last + 1).let { it..it })
    is BinOpsList.As -> BinOpsList.Error(postfixOperationOnTypeError, (type.area.last + 1).let { it..it })
    is BinOpsList.Error -> this
}

enum class AsHandling { Safe, Nullable, Unsafe }

object ContinueExpr : Expression()
data class BreakExpr(val expr: PExpr?) : Expression()
data class ReturnExpr(val expr: PExpr?) : Expression()

fun <R> scope() = scope as Parser<R, ExprScope>

private val scope: Parser<Any, ExprScope> = requireChar<Any>('{', missingScopeError).takeR { state, succ, cErr, _ ->
    handleScopeBody(state.nextActual, emptyList(), succ, cErr)
}

fun <R> scopeExpr() = scopeExpr as Parser<R, ScopeExpr>

private val scopeExpr: Parser<Any, ScopeExpr> =
    tryChar<Any>('{', missingScopeError).takeR { state, succ: SuccFn<Any, ExprScope>, cErr, _ ->
        handleScopeBody(state.actual, emptyList(), succ, cErr)
    }.map(::ScopeExpr)

fun <R> handleScopeBody(
    state: ParseState,
    statements: List<ScopeStatement>,
    succFn: SuccFn<R, ExprScope>,
    errFn: ErrFn<R>
): R = when (state.char) {
    null -> errFn(unclosedScopeError, state.area)
    '}' -> succFn(ExprScope(statements), state.next)
    ';' -> handleScopeBody(state.next, statements, succFn, errFn)
    else -> when (val res = returnResult(state, scopeStatement())) {
        is ParseResult.Success -> {
            val sepState = res.next.actualOrBreak
            when (sepState.char) {
                ';', '\n' -> handleScopeBody(sepState.nextActual, statements + res.value, succFn, errFn)
                else -> errFn(unclosedScopeError, sepState.area)
            }
        }
        is ParseResult.Error -> errFn(res.error, res.area)
    }
}

private val scopeStatement: Parser<Any, ScopeStatement> by lazy {
    oneOf(
        normalIdentifier<Any>().withPos().flatMap { ident ->
            when (ident.value) {
                "val" -> valDeclaration
                "var" -> varDeclaration
                else -> scopedIdentExpr<Any>(ident).map(::ExprStatement)
            }
        },
        scopedExpr.map(::ExprStatement)
    )
}

fun <R> scopeStatement() = scopeStatement as Parser<R, ScopeStatement>

private val valDeclaration by lazy {
    whitespace<Any>()
        .takeR(identifier<Any>().withPos().failIf(::keywordDecNameError) { it in keywords })
        .zipWith2(typeAnnotation<Any>().orNull(), equalsScopedExpr) { name, type, expr ->
            VarDecStatement(name, false, type, expr)
        }
}

private val varDeclaration by lazy {
    whitespace<Any>()
        .takeR(identifier<Any>().withPos().failIf(::keywordDecNameError) { it in keywords })
        .zipWith2(typeAnnotation<Any>().orNull(), equalsScopedExpr) { name, type, expr ->
            VarDecStatement(name, true, type, expr)
        }
}

private val asHandling: Parser<Any, AsHandling> = { state, succ, _, _ ->
    when (state.char) {
        '!' -> succ(AsHandling.Unsafe, state.next)
        '?' -> succ(AsHandling.Nullable, state.next)
        else -> succ(AsHandling.Safe, state.next)
    }
}

private fun directOp(termP: Parser<Any, Expression>): Parser<Any, (BinOpsList) -> BinOpsList> =
    symbol<Any>().withPos().flatMap { op ->
        val symbol = op.value
        if (symbol.length == 2 && symbol.last() == '.') whitespace<Any>().takeR(termP).withPos().map { expr ->
            { list: BinOpsList -> list.appendSymbol(op, expr) }
        } else oneOf(
            termP.withPos().map { expr ->
                { list: BinOpsList -> list.appendSymbol(op, expr) }
            }, valueP { list: BinOpsList -> list.map { PostfixOpExpr(op, it) at it.area.first..op.area.last } }
        )
    }

private fun <R> prefixOp(
    termP: () -> Parser<R, Expression>
): Parser<R, Expression> = symbol<R>().withPos().flatMap { op ->
    termP().withPos().map { term -> PostfixOpExpr(op, term) }.orDefault(OpRefExpr(op.value))
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

private val list: Parser<Any, Expression> = tryChar<Any>('[').takeR { state, succ, cErr, _ ->
    handleList(state, emptyList(), emptyList(), state.pos, succ, cErr)
}

fun <R> list() = list as Parser<R, Expression>

private fun <R> handleList(
    state: ParseState,
    exprList: List<PExpr>,
    sections: List<Positioned<ListExpr>>,
    lastStart: StringPos,
    succFn: SuccFn<R, Expression>,
    errFn: ErrFn<R>
): R = if (state.char == ']') {
    finishList(sections, exprList, lastStart, state, succFn)
} else when (val res = returnResult(state, inlineExpr())) {
    is ParseResult.Success -> {
        val symbolState = res.next.actual
        val newExprList = exprList + res.value
        when (symbolState.char) {
            ']' -> finishList(sections, newExprList, lastStart, symbolState, succFn)
            ',' -> handleList(symbolState.nextActual, newExprList, sections, lastStart, succFn, errFn)
            ';' -> handleList(
                symbolState.nextActual, emptyList(),
                sections + (ListExpr(exprList + res.value) at lastStart..symbolState.pos),
                symbolState.pos, succFn, errFn
            )
            else -> errFn(unclosedSquareBacketError, symbolState.area)
        }
    }
    is ParseResult.Error -> errFn(res.error, res.area)
}

private fun <R> finishList(
    sections: List<Positioned<ListExpr>>,
    expressions: List<PExpr>,
    lastStart: StringPos,
    endState: ParseState,
    succFn: SuccFn<R, Expression>
): R = succFn(
    ListExpr(
        if (sections.isEmpty()) expressions
        else sections + (ListExpr(expressions) at lastStart..endState.pos)
    ), endState.next
)

fun <R> identTerm(
    ident: String,
    whitespaceP: Parser<R, Unit>,
    binOpsP: Parser<R, PExpr>
): Parser<R, Expression> = when (ident) {
    "true" -> trueConst()
    "false" -> falseConst()
    "null" -> nullConst()
    "this" -> thisConst()
    "continue" -> continueConst()
    "return" -> containerExpr(whitespaceP, binOpsP, ::ReturnExpr)
    "break" -> containerExpr(whitespaceP, binOpsP, ::BreakExpr)
    "if" -> if_()
    "for" -> for_()
    "when" -> when_()
    else -> valueP(IdentExpr(ident))
}

private fun <R> containerExpr(
    whitespaceP: Parser<R, Unit>,
    binOpsP: Parser<R, PExpr>,
    exprFn: (PExpr?) -> Expression
) = whitespaceP.takeR(binOpsP).orNull().map(exprFn)

private fun <R> term(
    whitespaceP: Parser<R, Unit>,
    termP: () -> Parser<R, Expression>,
    binOpsP: () -> Parser<R, PExpr>
) = oneOfOrError(
    missingExpressionError,
    number(),
    char(),
    string(),
    prefixOp(termP),
    list(),
    tickedIdentifier<R>().map(::IdentExpr),
    normalIdentifier<R>().flatMap { identTerm(it, whitespaceP, binOpsP()) }
)

private fun binOpsBody(
    whitespaceP: Parser<Any, Unit>,
    termP: Parser<Any, Expression>,
    typeP: Parser<Any, PType>,
    excludeCurly: Boolean
): Parser<Any, (BinOpsList) -> BinOpsList> = oneOf(
    directOp(termP),
    whitespaceP.takeR(
        oneOf(
            normalIdentifier<Any>().withPos().flatMap { ident ->
                when (ident.value) {
                    "is" -> whitespace<Any>().takeR(typeP).map { type ->
                        { list: BinOpsList -> list.appendIs(type) }
                    }
                    "as" -> asHandling.zipWith(whitespace<Any>().takeR(typeP)) { safety, type ->
                        { list -> list.appendAs(type, safety) }
                    }
                    in keywords -> emptyError(infixKeywordError)
                    else -> whitespace<Any>().takeR(termP.withPos()).map { term ->
                        { list -> list.appendIdent(ident, term) }
                    }
                }
            },
            tickedIdentifier<Any>()
                .withPos()
                .takeL(whitespace())
                .zipWith(termP.withPos()) { ident, expr ->
                    { list -> list.appendIdent(ident, expr) }
                },
            symbol<Any>().withPos().failIf({ arrowOpError }) { it == "->" }
                .zipWith(whitespace<Any>().takeR(termP.withPos())) { op, term ->
                    { list: BinOpsList -> list.appendSymbol(op, term) }
                },
            invocation<Any>(excludeCurly).withPos().map { args ->
                { list ->
                    list.map { expr -> CallExpr(expr, args.value) at expr.area.first..args.area.last }
                }
            },
            *if (excludeCurly) {
                emptyArray()
            } else {
                arrayOf(whitespace<Any>().takeR(lambda.withPos()).map { lambda ->
                    { list ->
                        list.map { expr ->
                            CallExpr(expr, CallArgs(emptyList(), lambda)) at expr.area.first..lambda.area.last
                        }
                    }
                })
            }
        )
    )
)

private val scopedTerm: Parser<Any, Expression> by lazy {
    term(whitespaceOnLine(), { scopedTerm }, { scopedExpr() })
}

private val scopedBinOpsBody: Parser<Any, (BinOpsList) -> BinOpsList> by lazy {
    binOpsBody(whitespaceOnLine(), scopedTerm, scopedType<Any>().withPos(), false)
}

fun <R> scopedBinOpsBody() = scopedBinOpsBody as Parser<R, (BinOpsList) -> BinOpsList>

private tailrec fun <R> handleScopedBinOpsBody(
    list: BinOpsList,
    state: ParseState,
    succFn: SuccFn<R, PExpr>,
    errFn: ErrFn<R>,
    startPos: StringPos
): R = when (val res = returnResultT(state, scopedBinOpsBody())) {
    is ParseResultT.Success -> handleScopedBinOpsBody(res.value(list), res.next, succFn, errFn, startPos)
    is ParseResultT.Failure -> succFn(list.toExpr(startPos..state.lastPos), state)
    is ParseResultT.Error -> errFn(res.error, res.area)
}

private val inlineTerm: Parser<Any, Expression> by lazy {
    term(whitespace(), { inlineTerm }, { inlineExpr() })
}

private val inlineBinOpsBody: Parser<Any, (BinOpsList) -> BinOpsList> by lazy {
    binOpsBody(whitespace(), inlineTerm, inlineType<Any>().withPos(), false)
}

fun <R> inlineBinOpsBody() = inlineBinOpsBody as Parser<R, (BinOpsList) -> BinOpsList>

private tailrec fun <R> handleInlineBinOpsBody(
    list: BinOpsList,
    state: ParseState,
    succFn: SuccFn<R, PExpr>,
    errFn: ErrFn<R>,
    startPos: StringPos
): R = when (val res = returnResultT(state, inlineBinOpsBody())) {
    is ParseResultT.Success -> handleInlineBinOpsBody(res.value(list), res.next, succFn, errFn, startPos)
    is ParseResultT.Failure -> succFn(list.toExpr(startPos..state.lastPos), state)
    is ParseResultT.Error -> errFn(res.error, res.area)
}

private val inlineNoCurlyTerm: Parser<Any, Expression> by lazy {
    term(whitespace(), { inlineNoCurlyTerm }, { inlineNoCurlyExpr() })
}

private val inlineNoCurlyBinOpsBody: Parser<Any, (BinOpsList) -> BinOpsList> by lazy {
    binOpsBody(whitespace(), inlineNoCurlyTerm, inlineType<Any>().withPos(), true)
}

fun <R> inlineNoCurlyBinOpsBody() = inlineNoCurlyBinOpsBody as Parser<R, (BinOpsList) -> BinOpsList>

private tailrec fun <R> handleInlineNoCurlyBinOpsBody(
    list: BinOpsList,
    state: ParseState,
    succFn: SuccFn<R, PExpr>,
    errFn: ErrFn<R>,
    startPos: StringPos
): R = when (val res = returnResultT(state, inlineNoCurlyBinOpsBody())) {
    is ParseResultT.Success -> handleInlineNoCurlyBinOpsBody(res.value(list), res.next, succFn, errFn, startPos)
    is ParseResultT.Failure -> succFn(list.toExpr(startPos..state.lastPos), state)
    is ParseResultT.Error -> errFn(res.error, res.area)
}

fun <R> invocation(excludeCurly: Boolean) = tryChar<R>('(')
    .takeR(loopingBodyParser(')', callArg(), unclosedParenthesisError))
    .let {
        if (excludeCurly) it.map(::CallArgs)
        else it.zipWith(whitespace<R>().takeR(lambda<R>().withPos()).orNull(), ::CallArgs)
    }

fun <R> callArg(): Parser<R, Arg> = oneOf(
    identifier<R>().withPos().flatMap { ident ->
        equalsInlineExpr<R>().map { expr -> Arg.Named(ident, expr) }.or(inlineIdentExpr<R>(ident).map(Arg::Free))
    }, inlineExpr<R>().map(Arg::Free)
)

fun <R> inlineIdentExpr(ident: PString): Parser<R, PExpr> = identTerm<R>(ident.value, whitespace(), inlineExpr())
    .flatMap { term ->
        { state, succ: SuccFn<R, PExpr>, _, _ -> succ(term at ident.area.first..state.lastPos, state) }
    }.flatMap {
        { state, succ, cErr, _ -> handleInlineBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
    }

fun <R> inlineNoCurlyIdentExpr(ident: PString): Parser<R, PExpr> = identTerm<R>(ident.value, whitespace(), inlineNoCurlyExpr())
        .flatMap { term ->
            { state, succ: SuccFn<R, PExpr>, _, _ -> succ(term at ident.area.first..state.lastPos, state) }
        }.flatMap {
            { state, succ, cErr, _ -> handleInlineNoCurlyBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
        }

fun <R> scopedIdentExpr(ident: PString): Parser<R, PExpr> = identTerm<R>(ident.value, whitespace(), scopedExpr())
    .flatMap { term ->
        { state, succ: SuccFn<R, PExpr>, _, _ -> succ(term at ident.area.first..state.lastPos, state) }
    }.flatMap {
        { state, succ, cErr, _ -> handleScopedBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
    }

fun <R> inlineExpr(): Parser<R, PExpr> = inlineExpr as Parser<R, PExpr>
private val inlineExpr: Parser<Any, PExpr> = inlineTerm.withPos().flatMap {
    { state, succ, cErr, _ -> handleInlineBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
}

fun <R> inlineNoCurlyExpr(): Parser<R, PExpr> = inlineNoCurlyExpr as Parser<R, PExpr>
private val inlineNoCurlyExpr: Parser<Any, PExpr> = inlineNoCurlyTerm.withPos().flatMap {
    { state, succ, cErr, _ -> handleInlineNoCurlyBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
}

fun <R> scopedExpr(): Parser<R, PExpr> = scopedExpr as Parser<R, PExpr>
private val scopedExpr: Parser<Any, PExpr> = scopedTerm.withPos().flatMap {
    { state, succ, cErr, _ -> handleScopedBinOpsBody(BinOpsList.Head(it), state.actual, succ, cErr, it.area.first) }
}

fun <R> equalsScopedExpr() = equalsScopedExpr as Parser<R, PExpr>
private val equalsScopedExpr: Parser<Any, PExpr> = whitespace<Any>()
    .takeR(tryChar('='))
    .takeR(whitespace())
    .takeR(scopedExpr())

fun <R> equalsInlineExpr() = equalsInlineExpr as Parser<R, PExpr>
private val equalsInlineExpr: Parser<Any, PExpr> = whitespace<Any>()
    .takeR(tryChar('='))
    .takeR(whitespace())
    .takeR(scopedExpr())

private val valCond = whitespace<Any>()
    .takeR(valPatternNoExpr()).zipWith(
        whitespace<Any>()
            .takeR(requireSymbol("="))
            .takeR(whitespace())
            .takeR(inlineExpr()),
        ::DecCondition
    )

private val varCond = whitespace<Any>()
    .takeR(varPatternNoExpr()).zipWith(
        whitespace<Any>()
            .takeR(requireSymbol("="))
            .takeR(whitespace())
            .takeR(inlineExpr()),
        ::DecCondition
    )

fun <R> condition() = condition as Parser<R, Condition>

private val condition: Parser<Any, Condition> = oneOfOrError(
    missingConditionError,
    normalIdentifier<Any>().withPos().flatMap { ident ->
        when (ident.value) {
            "val" -> valCond
            "var" -> varCond
            else -> inlineNoCurlyIdentExpr<Any>(ident).map(::ExprCondition)
        }
    },
    inlineNoCurlyExpr.map(::ExprCondition)
)

private val conditions: Parser<Any, List<Condition>> = parser@{ startState, succ, cErr, _ ->
    loopValue(emptyList<Condition>() to startState.actual) { (list, state) ->
        when (val res = returnResult(state, condition())) {
            is ParseResult.Success -> {
                val commaState = res.next.actual
                if (commaState.char == ',') list + res.value to commaState.next
                else return@parser succ(list + res.value, commaState)
            }
            is ParseResult.Error -> return@parser cErr(res.error, res.area)
        }
    }
}

fun <R> if_() = if_ as Parser<R, Expression>

private val if_ = conditions.zipWith2(
    scope,
    whitespace<Any>().takeR(tryIdent("else")).takeR(whitespace()).takeR(scope).orDefault(null),
    ::IfExpr
)

fun <R> for_() = for_ as Parser<R, Expression>

private val for_: Parser<Any, Expression> = whitespace<Any>()
    .takeR(identifier<Any>().withPos())
    .zipWith2(
        whitespace<Any>().takeR(
            requireIdent<Any>("in", missingInInForError).takeR(whitespace()).takeR(inlineNoCurlyExpr())
        ),
        whitespace<Any>().takeR(scope()),
        ::ForExpr
    )

fun <R> when_() = when_ as Parser<R, Expression>

private val when_ = whitespace<Any>().takeR(oneOf(
    tryChar<Any>('{')
        .takeR { state, succ: SuccFn<Any, List<WhenBranch>>, cErr, _ ->
            handleWhenBody(state.actual, emptyList(), succ, cErr)
        }.map { WhenExpr(null, it) },
    inlineNoCurlyExpr.takeL(whitespace()).takeL(requireChar('{', missingScopeError)).zipWith(
        { state, succ: SuccFn<Any, List<WhenBranch>>, cErr, _ ->
            handleWhenBody(state.actual, emptyList(), succ, cErr)
        }, ::WhenExpr
    )
)
)

private fun <R> handleWhenBody(
    state: ParseState,
    branches: List<WhenBranch>,
    succFn: SuccFn<R, List<WhenBranch>>,
    errFn: ErrFn<R>
): R = when (state.char) {
    null -> errFn(unclosedWhenError, state.area)
    '}' -> succFn(branches, state.next)
    ';' -> handleWhenBody(state.next, branches, succFn, errFn)
    else -> when (val res = returnResult(state, whenBranch())) {
        is ParseResult.Success -> {
            val sepState = res.next.actualOrBreak
            when (sepState.char) {
                ';', '\n' -> handleWhenBody(sepState.nextActual, branches + res.value, succFn, errFn)
                else -> errFn(unclosedWhenError, sepState.area)
            }
        }
        is ParseResult.Error -> errFn(res.error, res.area)
    }
}

fun <R> whenBranch() = whenBranch as Parser<R, WhenBranch>

private val whenBranch: Parser<Any, WhenBranch> = pattern<Any>()
    .takeL(whitespace())
    .takeL(requireSymbol("->"))
    .takeL(whitespace())
    .zipWith(oneOfOrError(missingExpressionError, scopeExpr.withPos(), scopedExpr), PPattern::to)

private val lambda: Parser<Any, Expression> = tryChar<Any>('{')
    .takeR(whitespace())
    .takeR parser@{ startState, succ: SuccFn<Any, LambdaParams>, cErr, _ ->
        loopValue(emptyList<Pair<PString, PType?>>() to startState.actual) { (list, state) ->
            if (state.startsWithSymbol("->")) {
                return@parser succ(list, state.next)
            } else when (val res = returnResult(state, nullableTypedParam())) {
                is ParseResult.Success -> {
                    val symbolState = res.next.actual
                    when {
                        symbolState.char == ',' -> list + res.value to symbolState.next
                        symbolState.startsWithSymbol("->") -> return@parser succ(list + res.value, symbolState + 2)
                        else -> return@parser cErr(invalidLambdaArgumentsError, symbolState.area)
                    }
                }
                is ParseResult.Error -> return@parser cErr(res.error, res.area)
            }
        }
    }
    .orDefault(emptyList())
    .zipWith({ state, succ, cErr, _ -> handleScopeBody(state, emptyList(), succ, cErr) }, ::LambdaExpr)

fun <R> lambda() = lambda as Parser<R, Expression>

private val nullableTypedParam: Parser<Any, Pair<PString, PType?>> = identifier<Any>()
    .withPos()
    .zipWith(typeAnnotation<Any>().orNull()) { name, type -> name to type }

fun <R> nullableTypedParam() = nullableTypedParam as Parser<R, Pair<PString, PType?>>