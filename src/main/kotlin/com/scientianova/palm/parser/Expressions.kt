package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.*

sealed class Expression : IStatement
typealias PExpression = Positioned<Expression>

data class PathExpr(
    val parts: List<PString>
) : Expression()

data class CallExpr(
    val expr: PExpression,
    val params: List<PExpression>
) : Expression()

data class LambdaExpr(
    val params: List<PDecPattern>,
    val expr: PExpression
) : Expression()

data class WhenExpr(
    val comparsing: PExpression,
    val branches: List<Pair<PExpression, PExpression>>
) : Expression()

data class DecExpr(
    val pattern: PExpression,
    val value: PExpression?
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
    val components: List<PExpression>
) : Expression()

data class ListExpr(
    val components: List<PExpression>,
    val mutable: Boolean
) : Expression()

data class ArrayExpr(
    val components: List<PExpression>
) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String) : BinOp()

data class PrefixOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class BinaryOpsExpr(val body: List<Pair<PExpression, PBinOp>>, val end: PExpression) : Expression()

data class TypeExpr(val expr: PExpression, val type: PType) : Expression()

@Suppress("NON_TAIL_RECURSIVE_CALL")
tailrec fun handleScope(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    returnBracket: Boolean,
    statements: List<PStatement> = emptyList()
): Pair<PExpression, PToken?> = when (token?.value) {
    is ClosedCurlyBracketToken ->
        if (returnBracket) ScopeExpr(statements) on start..token.area.end to token
        else ScopeExpr(statements) on start..token.area.end to parser.pop()
    is ImportToken -> {
        val (import, next) = handleImportStart(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleScope(actualNext, parser, start, returnBracket, statements + import)
    }
    is LetToken -> {
        val (declaration, next) = handleDeclaration(parser.pop(), parser, token.area.start)
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
                val (scope, _) = handleScope(actualAfterExpr, parser, expr.area.start, false)
                val area = expr.area.start..scope.area.end
                val statement = BinaryOpsExpr(
                    listOf(fromExpr to (SymbolOp(">>=") on next.area)),
                    LambdaExpr(actualPattern, scope) on area
                ) on area
                if (returnBracket) ScopeExpr(statements + statement) on start..scope.area.end to token
                else ScopeExpr(statements + statement) on start..scope.area.end to parser.pop()
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
                            FunctionAssignment(name, expr.value.params, funExpr, false) on
                                    expr.area.start..funExpr.area.end)
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
                    statements + (ConstAssignment(pattern, valueExpr, false) on expr.area.start..valueExpr.area.end)
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
    token: PToken?,
    parser: Parser,
    ignoreBreak: Boolean = false,
    inScope: Boolean = false,
    excludeCurly: Boolean = false
): Pair<PExpression, PToken?> {
    val (firstPart, next) = handleExpressionPart(token, parser, ignoreBreak, inScope, excludeCurly)
    return handleBinOps(next, parser, firstPart, ignoreBreak, inScope, excludeCurly)
}

tailrec fun handleBinOps(
    token: PToken?,
    parser: Parser,
    last: PExpression,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean,
    body: List<Pair<PExpression, PBinOp>> = emptyList()
): Pair<PExpression, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> if (ignoreBreak || token.area.start.row == last.area.end.row) {
        val (part, next) = handleExpressionPart(parser.pop(), parser, ignoreBreak, inScope, excludeCurly)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (InfixCall(value.name) on token.area))
        )
    } else
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) on body.first().first.area.start..last.area.end) to token
    is InfixOperatorToken -> {
        val (part, next) = handleExpressionPart(parser.pop(), parser, ignoreBreak, inScope, excludeCurly)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (SymbolOp(value.symbol) on token.area))
        )
    }
    else ->
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) on body.first().first.area.start..last.area.end) to token
}

fun handleExpressionPart(
    token: PToken?,
    parser: Parser,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean
): Pair<PExpression, PToken?> {
    val (part, next) = when (val value = token?.value) {
        is ByteToken -> ByteExpr(value.value) on token.area to parser.pop()
        is ShortToken -> ShortExpr(value.value) on token.area to parser.pop()
        is IntToken -> IntExpr(value.value) on token.area to parser.pop()
        is LongToken -> LongExpr(value.value) on token.area to parser.pop()
        is FloatToken -> FloatExpr(value.value) on token.area to parser.pop()
        is DoubleToken -> DoubleExpr(value.value) on token.area to parser.pop()
        is CharToken -> CharExpr(value.char) on token.area to parser.pop()
        is WildcardToken -> WildcardExpr on token.area to parser.pop()
        is PureStringToken -> StringExpr(value.text) on token.area to parser.pop()
        is StringTemplateToken -> {
            val parts = value.parts.map { (part, area) ->
                when (part) {
                    is StringPart -> StringExpr(part.string) on area
                    is TokensPart -> {
                        val interParser = parser.handle(part.tokens)
                        handleInterpolation(interParser.pop(), interParser, area)
                    }
                } to (SymbolOp("+") on area.end)
            }
            BinaryOpsExpr(parts.dropLast(1), parts.last().first) on token.area to parser.pop()
        }
        MUT_TOKEN -> {
            val next = parser.pop()
            if (next?.value is OpenSquareBracketToken)
                handleList(parser.pop(), parser, next.area.start, true, emptyList(), emptyList())
            else handleIdentifiers("mut" on token.area, parser)
        }
        is IdentifierToken -> handleIdentifiers(value.name on token.area, parser)
        is OpenParenToken -> handleParenthesizedExpr(parser.pop(), parser, token.area.start)
        is OpenSquareBracketToken -> handleList(parser.pop(), parser, token.area.start, false, emptyList(), emptyList())
        is OpenArrayBracketToken -> handleArray(parser.pop(), parser, token.area.start, emptyList(), emptyList())
        is OpenCurlyBracketToken -> handleScope(parser.pop(), parser, token.area.start, false)
        is PrefixOperatorToken -> {
            val (expr, next) = handleExpressionPart(parser.pop(), parser, ignoreBreak, inScope, excludeCurly)
            PrefixOpExpr(value.symbol on token.area, expr) on token.area.start..expr.area.end to next
        }
        is WhenToken -> {
            val next = parser.pop()
            if (next?.value is OpenCurlyBracketToken) handleWhen(
                parser.pop(), parser, PathExpr(listOf("True" on next.area)) on next.area,
                token.area.start, emptyList()
            ) else {
                val (comparing, bracket) = handleExpression(next, parser, excludeCurly = true)
                if (bracket?.value is OpenCurlyBracketToken) handleWhen(
                    parser.pop(), parser, comparing, token.area.start, emptyList()
                ) else parser.error(MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR, bracket?.area?.start ?: parser.lastPos)
            }
        }
        is LetToken -> {
            val (pattern, equals) = handleExpression(parser.pop(), parser, ignoreBreak, excludeCurly = excludeCurly)
            if (equals?.value is EqualsToken) {
                val (expr, next) = handleExpression(parser.pop(), parser, ignoreBreak, excludeCurly = excludeCurly)
                DecExpr(pattern, expr) on token.area.start..expr.area.end to next
            } else DecExpr(pattern, null) on token.area.start..pattern.area.end to equals
        }
        else -> parser.error(INVALID_EXPRESSION_ERROR, token?.area ?: parser.lastArea)
    }
    return handlePostfix(next, parser, part, ignoreBreak, inScope, excludeCurly)
}

tailrec fun handlePostfix(
    token: PToken?,
    parser: Parser,
    expr: PExpression,
    ignoreBreak: Boolean,
    inScope: Boolean,
    excludeCurly: Boolean
): Pair<PExpression, PToken?> = when (val value = token?.value) {
    is PostfixOperatorToken -> handlePostfix(
        parser.pop(), parser,
        PostfixOpExpr(value.symbol on token.area, expr) on expr.area.start..token.area.end,
        ignoreBreak, inScope, excludeCurly
    )
    is OpenParenToken -> if (ignoreBreak || expr.area.end.row == token.area.start.row) {
        val (params, next) = handleParams(parser.pop(), parser, token.area.start)
        handlePostfix(
            next, parser, CallExpr(expr, params.value) on expr.area.start..params.area.end,
            ignoreBreak, inScope, excludeCurly
        )
    } else expr to token
    is OpenCurlyBracketToken -> if (excludeCurly) expr to token else {
        val (scope, next) = handleScope(parser.pop(), parser, token.area.start, false)
        handlePostfix(
            next, parser,
            CallExpr(expr, listOf(scope)) on expr.area.start..scope.area.end,
            ignoreBreak, inScope, excludeCurly
        )
    }
    is RightArrowToken -> {
        val input = exprToDecPattern(expr, parser, INVALID_PARAMETER_ERROR)
        val actualInput = if (input.value is DecTuplePattern) input.value.values else listOf(input)
        val (lamExpr, next) =
            if (inScope) handleScope(parser.pop(), parser, token.area.start, true)
            else handleExpression(parser.pop(), parser, ignoreBreak)
        handlePostfix(
            next, parser,
            LambdaExpr(actualInput, lamExpr) on expr.area.start..lamExpr.area.end,
            ignoreBreak, inScope, excludeCurly
        )
    }
    is ColonToken -> {
        val (type, next) = handleType(parser.pop(), parser)
        TypeExpr(expr, type) on expr.area.start..type.area.end to next
    }
    else -> expr to token
}

tailrec fun handleParams(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    expressions: List<PExpression> = emptyList()
): Pair<Positioned<List<PExpression>>, PToken?> = if (token?.value is ClosedParenToken)
    (if (expressions.isEmpty()) listOf(UnitExpr on start..token.area.end) else expressions) on
            start..token.area.end to parser.pop()
else {
    val (expr, symbol) = handleExpression(token, parser, true)
    when (symbol?.value) {
        is ClosedParenToken -> expressions + expr on start..symbol.area.end to parser.pop()
        is CommaToken -> handleParams(parser.pop(), parser, start, expressions + expr)
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleInterpolation(
    token: PToken?,
    parser: Parser,
    area: StringArea,
    statements: List<PStatement> = emptyList()
): PExpression = when (token?.value) {
    null -> ScopeExpr(statements) on area
    is ImportToken -> {
        val (import, next) = handleImportStart(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleInterpolation(actualNext, parser, area, statements + import)
    }
    is LetToken -> {
        val (declaration, next) = handleDeclaration(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleInterpolation(actualNext, parser, area, statements + declaration)
    }
    else -> {
        val (expr, next) = handleExpression(token, parser)
        when (next?.value) {
            is LeftArrowToken -> {
                val pattern = exprToDecPattern(expr, parser, INVALID_PARAMETER_ERROR)
                val actualPattern = if (pattern.value is DecTuplePattern) pattern.value.values else listOf(pattern)
                val (fromExpr, afterExpr) = handleExpression(parser.pop(), parser)
                val actualAfterExpr = when (afterExpr?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> afterExpr
                }
                val (scope, _) = handleScope(actualAfterExpr, parser, expr.area.start, false)
                val lambdaArea = expr.area.start..scope.area.end
                val statement = BinaryOpsExpr(
                    listOf(fromExpr to (SymbolOp(">>=") on next.area)),
                    LambdaExpr(actualPattern, scope) on lambdaArea
                ) on lambdaArea
                ScopeExpr(statements + statement) on area
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
                handleInterpolation(
                    actualNext, parser, area, statements + (
                            FunctionAssignment(name, expr.value.params, funExpr, false) on
                                    expr.area.start..funExpr.area.end)
                )
            } else {
                val pattern = exprToDecPattern(expr, parser, INVALID_DESTRUCTURED_DECLARATION_ERROR)
                val (valueExpr, next1) = handleExpression(parser.pop(), parser)
                val actualNext = when (next1?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next1
                }
                handleInterpolation(
                    actualNext, parser, area,
                    statements + (ConstAssignment(pattern, valueExpr, false) on expr.area.start..valueExpr.area.end)
                )
            }
            else -> {
                val actualNext = when (next?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next
                }
                handleInterpolation(actualNext, parser, area, statements + expr)
            }
        }
    }
}

tailrec fun handleIdentifiers(
    token: PString,
    parser: Parser,
    path: List<PString> = emptyList()
): Pair<Positioned<PathExpr>, PToken?> {
    val symbol = parser.pop()
    return if (symbol?.value is DotToken) {
        val (name, pos) = parser.pop() ?: parser.error(INVALID_PATH_ERROR, parser.lastPos)
        handleIdentifiers(
            (name as? IdentifierToken ?: parser.error(INVALID_PATH_ERROR, parser.lastPos)).name on pos,
            parser, path + token
        )
    } else PathExpr(path + token) on (path.firstOrNull() ?: token).area.start..token.area.end to symbol
}

tailrec fun handleParenthesizedExpr(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    expressions: List<PExpression> = emptyList()
): Pair<PExpression, PToken?> = if (token?.value is ClosedParenToken) when (expressions.size) {
    0 -> UnitExpr on start..token.area.end
    1 -> expressions.first()
    else -> TupleExpr(expressions) on start..token.area.end
} to parser.pop() else {
    val (expr, symbol) = handleExpression(token, parser, true)
    when (symbol?.value) {
        is ClosedParenToken -> (if (expressions.isEmpty()) expr else TupleExpr(expressions + expr) on start..symbol.area.end) to parser.pop()
        is CommaToken -> handleParenthesizedExpr(parser.pop(), parser, start, expressions + expr)
        else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleList(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    mutable: Boolean,
    expressions: List<PExpression>,
    sections: List<Positioned<ListExpr>>,
    lastStart: StringPos = start
): Pair<PExpression, PToken?> = if (token?.value is ClosedSquareBracketToken)
    ListExpr(
        if (sections.isEmpty()) expressions else sections + (ListExpr(
            expressions,
            mutable
        ) on lastStart..token.area.end), mutable
    ) on start..token.area.end to parser.pop()
else {
    val (expr, symbol) = handleExpression(token, parser, true)
    val newExpressions = expressions + expr
    when (symbol?.value) {
        is ClosedSquareBracketToken ->
            ListExpr(
                if (sections.isEmpty()) newExpressions else sections + (ListExpr(newExpressions, mutable) on
                        lastStart..symbol.area.end), mutable
            ) on start..symbol.area.end to parser.pop()
        is CommaToken -> handleList(parser.pop(), parser, start, mutable, expressions + expr, sections, lastStart)
        is SemicolonToken -> handleList(
            parser.pop(), parser, start, mutable, emptyList(),
            sections + (ListExpr(expressions + expr, mutable) on lastStart..symbol.area.end), symbol.area.end
        )
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}

fun handleArray(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    expressions: List<PExpression>,
    sections: List<Positioned<ArrayExpr>>,
    lastStart: StringPos = start
): Pair<PExpression, PToken?> = if (token?.value is ClosedArrayBracketToken)
    ArrayExpr(
        if (sections.isEmpty()) expressions else sections + (ArrayExpr(expressions) on
                lastStart..token.area.end)
    ) on start..token.area.end to parser.pop()
else {
    val (expr, symbol) = handleExpression(token, parser, true)
    val newExpressions = expressions + expr
    when (symbol?.value) {
        is ClosedArrayBracketToken ->
            ArrayExpr(
                if (sections.isEmpty()) newExpressions else sections + (ArrayExpr(
                    newExpressions
                ) on lastStart..symbol.area.end)
            ) on start..symbol.area.end to parser.pop()
        is CommaToken -> handleArray(parser.pop(), parser, start, expressions + expr, sections, lastStart)
        is SemicolonToken -> handleArray(
            parser.pop(), parser, start, emptyList(),
            sections + (ArrayExpr(expressions + expr) on lastStart..symbol.area.end), symbol.area.end
        )
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}

tailrec fun handleWhen(
    token: PToken?,
    parser: Parser,
    comparing: PExpression,
    start: StringPos,
    branches: List<Pair<PExpression, PExpression>>
): Pair<PExpression, PToken?> = if (token?.value is ClosedCurlyBracketToken)
    WhenExpr(comparing, branches) on start..token.area.end to parser.pop()
else {
    val (pattern, arrow) = handleExpression(token, parser)
    if (arrow?.value !is RightArrowToken) parser.error(MISSING_ARROW_ERROR, arrow?.area ?: parser.lastArea)
    val (result, next) = handleExpression(parser.pop(), parser)
    if (next?.value is SeparatorToken)
        handleWhen(parser.pop(), parser, comparing, start, branches + (pattern to result))
    else handleWhen(next, parser, comparing, start, branches + (pattern to result))
}