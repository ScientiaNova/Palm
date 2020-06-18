package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.*

sealed class Expression : IStatement
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
    val comparsing: PExpr,
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
    token: PToken?,
    parser: Parser,
    start: StringPos,
    returnBracket: Boolean,
    statements: List<PStatement> = emptyList()
): Pair<PExpr, PToken?> = when (token?.value) {
    is ClosedCurlyBracketToken ->
        if (returnBracket) ScopeExpr(statements) at start..token.area.last to token
        else ScopeExpr(statements) at start..token.area.last to parser.pop()
    is ImportToken -> {
        val (import, next) = handleImportStart(parser.pop(), parser, token.area.first)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleScope(actualNext, parser, start, returnBracket, statements + import)
    }
    is LetToken -> {
        val (declaration, next) = handleDeclaration(parser.pop(), parser, token.area.first)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleScope(actualNext, parser, start, returnBracket, statements + declaration)
    }
    else -> {
        val (expr, next) = handleExpression(token, parser, inScope = true)
        when (next?.value) {
            is LeftArrowToken -> {
                val pattern = exprToDecPattern(expr, parser, INVALID_PARAMETER_ERROR)
                val actualPattern = if (pattern.value is DecTuplePattern) pattern.value.values else listOf(pattern)
                val (fromExpr, afterExpr) = handleExpression(parser.pop(), parser)
                val actualAfterExpr = when (afterExpr?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> afterExpr
                }
                val (scope, _) = handleScope(actualAfterExpr, parser, expr.area.first, false)
                val area = expr.area.first..scope.area.last
                val statement = BinaryOpsExpr(
                    listOf(fromExpr to (SymbolOp(">>=") at next.area)),
                    LambdaExpr(actualPattern, scope) at area
                ) at area
                if (returnBracket) ScopeExpr(statements + statement) at start..scope.area.last to token
                else ScopeExpr(statements + statement) at start..scope.area.last to parser.pop()
            }
            is EqualsToken -> if (expr.value is CallExpr) {
                if (expr.value.expr.value !is PathExpr) parser.error(INVALID_FUNCTION_NAME, expr.value.expr.area)
                if (expr.value.expr.value.parts.size == 1) parser.error(INVALID_FUNCTION_NAME, expr.value.expr.area)
                val name = expr.value.expr.value.parts.first()
                val (funExpr, next1) = handleExpression(parser.pop(), parser)
                val actualNext = when (next1?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next1
                }
                handleScope(
                    actualNext, parser, start, returnBracket,
                    statements + (
                            FunctionAssignment(name, expr.value.params, funExpr, false) at
                                    expr.area.first..funExpr.area.last)
                )
            } else {
                val pattern = exprToDecPattern(expr, parser, INVALID_DESTRUCTURED_DECLARATION_ERROR)
                val (valueExpr, next1) = handleExpression(parser.pop(), parser)
                val actualNext = when (next1?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next1
                }
                handleScope(
                    actualNext, parser, start, returnBracket,
                    statements + (ConstAssignment(pattern, valueExpr, false) at expr.area.first..valueExpr.area.last)
                )
            }
            else -> {
                val actualNext = when (next?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next
                }
                handleScope(actualNext, parser, start, returnBracket, statements + expr)
            }
        }
    }
}

fun handleExpression(
    state: ParseState,
    ignoreBreak: Boolean = false,
    inScope: Boolean = false,
    excludeCurly: Boolean = false
): Pair<PExpr, ParseState> {
    val (firstPart, next) = handleSubexpression(state, ignoreBreak, inScope, excludeCurly)
    return handleBinOps(next.actualOrBreak, firstPart, ignoreBreak, inScope, excludeCurly)
}

tailrec fun handleBinOps(
    token: PToken?,
    parser: Parser,
    last: PExpr,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean,
    body: List<Pair<PExpr, PBinOp>> = emptyList()
): Pair<PExpr, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> if (ignoreBreak || parser.lineEnds.onSameRow(token.area.first, last.area.last)) {
        val (part, next) = handleSubexpression(parser.pop(), parser, ignoreBreak, inScope, excludeCurly)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (InfixCall(value.name) at token.area))
        )
    } else
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to token
    is InfixOperatorToken -> {
        val (part, next) = handleSubexpression(parser.pop(), parser, ignoreBreak, inScope, excludeCurly)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (SymbolOp(value.symbol) at token.area))
        )
    }
    else ->
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) at body.first().first.area.first..last.area.last) to token
}

fun handleSubexpression(
    state: ParseState,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean
): Pair<PExpr, ParseState> {
    val char = state.char
    fun Char.eq() = this == char
    val (subExpr, nextState) = when {
        char == null -> MISSING_EXPRESSION_ERROR throwAt state.lastPos
        char.isLetter() -> {
            val (ident, next) = handleIdentifier(state)
            when (ident.value) {
                "_" -> WildcardToken at state.lastPos to next
                "mut" -> {
                    val afterMut = next.nextActual
                    if (afterMut.char == '[')
                        handleList(afterMut.nextActual, afterMut.pos, true, emptyList(), emptyList())
                    else handleIdentifiers(next, ident, emptyList())
                }
                "when" -> {
                    if (next?.value is OpenCurlyBracketToken) handleWhen(
                        state.pop(), state, PathExpr(listOf("True" at next.area)) at next.area,
                        char.area.first, emptyList()
                    ) else {
                        val (comparing, bracket) = handleExpression(next, state, excludeCurly = true)
                        if (bracket?.value is OpenCurlyBracketToken) handleWhen(
                            state.pop(), state, comparing, char.area.first, emptyList()
                        ) else state.error(
                            MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR,
                            bracket?.area?.start ?: state.lastPos
                        )
                    }
                }
                "let" -> {
                    val (pattern, afterState) = handleExpression(
                        next.actual, ignoreBreak, excludeCurly = excludeCurly
                    )
                    val equalsState = afterState.actual
                    if (equalsState.char == '=') {
                        val (expr, returnState) = handleExpression(
                            equalsState.nextActual, ignoreBreak, excludeCurly = excludeCurly
                        )
                        DecExpr(pattern, expr) at state.pos..expr.area.last to returnState
                    } else DecExpr(pattern, null) at state.pos..pattern.area.last to afterState
                }
                else -> handleIdentifiers(next, ident, emptyList())
            }
        }
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
        } else handleList(state.nextActual, state.pos, false, emptyList(), emptyList())
        '{'.eq() -> handleScope(state.next, state.pos, false)
        char.isSymbolPart() -> {
            val (expr, next) = handleSubexpression(state.pop(), state, ignoreBreak, inScope, excludeCurly)
            PrefixOpExpr(value.symbol at char.area, expr) at char.area.first..expr.area.last to next
        }
        else -> INVALID_EXPRESSION_ERROR throwAt state.pos
    }
    return handlePostfix(nextState, state, subExpr, ignoreBreak, inScope, excludeCurly)
}

tailrec fun handlePostfix(
    token: PToken?,
    parser: Parser,
    expr: PExpr,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean
): Pair<PExpr, PToken?> = when (val value = token?.value) {
    is PostfixOperatorToken -> handlePostfix(
        parser.pop(), parser,
        PostfixOpExpr(value.symbol at token.area, expr) at expr.area.first..token.area.last,
        ignoreBreak, inScope, excludeCurly
    )
    is OpenParenToken -> if (ignoreBreak || parser.lineEnds.onSameRow(expr.area.last, token.area.first)) {
        val (params, next) = handleParams(parser.pop(), parser, token.area.first)
        handlePostfix(
            next, parser, CallExpr(expr, params.value) at expr.area.first..params.area.last,
            ignoreBreak, inScope, excludeCurly
        )
    } else expr to token
    is OpenCurlyBracketToken -> if (excludeCurly) expr to token else {
        val (scope, next) = handleScope(parser.pop(), parser, token.area.first, false)
        handlePostfix(
            next, parser,
            CallExpr(expr, listOf(scope)) at expr.area.first..scope.area.last,
            ignoreBreak, inScope, excludeCurly
        )
    }
    is RightArrowToken -> {
        val input = exprToDecPattern(expr, parser, INVALID_PARAMETER_ERROR)
        val actualInput = if (input.value is DecTuplePattern) input.value.values else listOf(input)
        val (lamExpr, next) =
            if (inScope) handleScope(parser.pop(), parser, token.area.first, true)
            else handleExpression(parser.pop(), parser, ignoreBreak)
        handlePostfix(
            next, parser,
            LambdaExpr(actualInput, lamExpr) at expr.area.first..lamExpr.area.last,
            ignoreBreak, inScope, excludeCurly
        )
    }
    is ColonToken -> {
        val (type, next) = handleType(parser.pop(), parser)
        TypeExpr(expr, type) at expr.area.first..type.area.last to next
    }
    else -> expr to token
}

tailrec fun handleParams(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    expressions: List<PExpr> = emptyList()
): Pair<Positioned<List<PExpr>>, PToken?> = if (token?.value is ClosedParenToken)
    (if (expressions.isEmpty()) listOf(UnitExpr at start..token.area.last) else expressions) at
            start..token.area.last to parser.pop()
else {
    val (expr, symbol) = handleExpression(token, parser, true)
    when (symbol?.value) {
        is ClosedParenToken -> expressions + expr at start..symbol.area.last to parser.pop()
        is CommaToken -> handleParams(parser.pop(), parser, start, expressions + expr)
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleIdentifiers(
    state: ParseState,
    last: PString,
    path: List<PString>
): Pair<Positioned<PathExpr>, ParseState> = if (state.char == '.') {
    val identState = state.next
    if (identState.char?.isLetter() != true) INVALID_PATH_ERROR throwAt state.pos
    val (ident, next) = handleIdentifier(identState)
    handleIdentifiers(next, ident, path + last)
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
    val (expr, afterState) = handleExpression(state, true)
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
    val (expr, afterState) = handleExpression(state, true)
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
    val (expr, afterState) = handleExpression(state, true)
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
    token: PToken?,
    parser: Parser,
    comparing: PExpr,
    start: StringPos,
    branches: List<Pair<PExpr, PExpr>>
): Pair<PExpr, PToken?> = if (token?.value is ClosedCurlyBracketToken) {
    WhenExpr(comparing, branches) at start..token.area.last to parser.pop()
} else {
    val (pattern, arrow) = handleExpression(token, parser)
    if (arrow?.value !is RightArrowToken) parser.error(MISSING_ARROW_ERROR, arrow?.area ?: parser.lastArea)
    val (result, next) = handleExpression(parser.pop(), parser)
    if (next?.value is SeparatorToken)
        handleWhen(parser.pop(), parser, comparing, start, branches + (pattern to result))
    else handleWhen(next, parser, comparing, start, branches + (pattern to result))
}