package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_PATH_ERROR
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.errors.UNCLOSED_SQUARE_BRACKET_ERROR
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

data class IfExpr(
    val conditions: PExpression,
    val thenScope: FunctionScope,
    val elseScope: FunctionScope?
) : Expression()

data class IfLetExpr(
    val conditions: PPattern,
    val thenScope: FunctionScope,
    val elseScope: FunctionScope?
) : Expression()

data class WhenExpr(
    val branches: List<Pair<PExpression, FunctionScope>>,
    val elseBranch: FunctionScope?
) : Expression()

data class WhenSwitchExpr(val branches: List<Pair<List<PPattern>, FunctionScope>>) : Expression()

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
    val components: List<PExpression>,
    val obj: Boolean
) : Expression()

sealed class BinOp
typealias PBinOp = Positioned<BinOp>

data class SymbolOp(val symbol: String) : BinOp()
data class InfixCall(val name: String) : BinOp()

data class PrefixOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class PostfixOpExpr(val symbol: PString, val expr: PExpression) : Expression()
data class BinaryOpsExpr(val body: List<Pair<PExpression, PBinOp>>, val end: PExpression) : Expression()

data class TypeExpr(val expr: PExpression, val type: PType) : Expression()

tailrec fun handleScope(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    statements: List<PStatement> = emptyList()
): Pair<PExpression, PToken?> = when (token?.value) {
    is ClosedCurlyBracketToken -> ScopeExpr(statements) on start..token.area.end to parser.pop()
    is ImportToken -> {
        val (import, next) = handleImport(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleScope(actualNext, parser, start, statements + import)
    }
    else -> {
        val (expr, next) = handleExpression(token, parser, inScope = true)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleScope(actualNext, parser, start, statements + expr)
    }
}

fun handleExpression(
    token: PToken?,
    parser: Parser,
    ignoreBreak: Boolean = false,
    inScope: Boolean = false,
    excludeCurly: Boolean = false
): Pair<PExpression, PToken?> {
    val (firstPart, next) = handleExpressionPart(token, parser)
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
    is IdentifierToken -> {
        val (part, next) = handleExpressionPart(parser.pop(), parser)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (InfixCall(value.name) on token.area))
        )
    }
    is InfixOperatorToken -> if (ignoreBreak || token.area.start == last.area.end) {
        val (part, next) = handleExpressionPart(parser.pop(), parser)
        handleBinOps(
            next, parser, part, ignoreBreak, inScope, excludeCurly,
            body + (last to (SymbolOp(value.symbol) on token.area))
        )
    } else
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) on body.first().first.area.start..last.area.end) to token
    else ->
        (if (body.isEmpty()) last
        else BinaryOpsExpr(body, last) on body.first().first.area.start..last.area.end) to token
}

fun handleExpressionPart(token: PToken?, parser: Parser): Pair<PExpression, PToken?> {
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
        is MutToken -> {
            val next = parser.pop()
            if (next?.value is OpenSquareBracketToken) handleList(parser.pop(), parser, next.area.start, true)
            else handleIdentifiers("mut" on token.area, parser)
        }
        is ObjectToken -> {
            val next = parser.pop()
            if (next?.value is OpenArrayBracketToken) handleArray(parser.pop(), parser, next.area.start, true)
            else handleIdentifiers("object" on token.area, parser)
        }
        is IdentifierToken -> handleIdentifiers(value.name on token.area, parser)
        is OpenParenToken -> handleParenthesizedExpr(parser.pop(), parser, token.area.start)
        is OpenSquareBracketToken -> handleList(parser.pop(), parser, token.area.start, false)
        is OpenArrayBracketToken -> handleArray(parser.pop(), parser, token.area.start, false)
        is OpenCurlyBracketToken -> handleScope(parser.pop(), parser, token.area.start)
        is PrefixOperatorToken -> {
            val (expr, next) = handleExpressionPart(parser.pop(), parser)
            PrefixOpExpr(value.symbol on token.area, expr) on token.area.start..expr.area.end to next
        }
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
        val (import, next) = handleImport(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleInterpolation(actualNext, parser, area, statements + import)
    }
    else -> {
        val (expr, next) = handleExpression(token, parser)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleInterpolation(actualNext, parser, area, statements + expr)
    }
}

tailrec fun handleIdentifiers(
    token: PString,
    parser: Parser,
    path: List<PString> = emptyList()
): Pair<PExpression, PToken?> {
    val symbol = parser.pop()
    return when (symbol?.value) {
        is DotToken -> {
            val (name, pos) = parser.pop() ?: parser.error(INVALID_PATH_ERROR, parser.lastPos)
            handleIdentifiers(
                (name as? IdentifierToken ?: parser.error(INVALID_PATH_ERROR, parser.lastPos)).name on pos,
                parser, path + token
            )
        }
        is EqualToToken -> {
            val (expr, next) = handleExpression(parser.pop(), parser)
            CallExpr(
                PathExpr(path + token.map { "set${it.capitalize()}" }) on
                        (path.firstOrNull() ?: token).area.start..token.area.end,
                listOf(expr)
            ) on token.area.start..expr.area.end to next
        }
        else -> PathExpr(path + token) on (path.firstOrNull() ?: token).area.start..token.area.end to parser.pop()
    }
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

fun handleList(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    mutable: Boolean,
    expressions: List<PExpression> = emptyList(),
    lastStart: StringPos = start,
    sections: List<Positioned<ListExpr>> = emptyList()
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
                if (sections.isEmpty()) newExpressions else sections + (ListExpr(
                    newExpressions,
                    mutable
                ) on lastStart..symbol.area.end), mutable
            ) on
                    start..symbol.area.end to parser.pop()
        is CommaToken -> handleList(parser.pop(), parser, start, mutable, expressions + expr, lastStart, sections)
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}

fun handleArray(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    obj: Boolean,
    expressions: List<PExpression> = emptyList(),
    lastStart: StringPos = start,
    sections: List<Positioned<ArrayExpr>> = emptyList()
): Pair<PExpression, PToken?> = if (token?.value is ClosedArrayBracketToken)
    ArrayExpr(
        if (sections.isEmpty()) expressions else sections + (ArrayExpr(
            expressions,
            obj
        ) on lastStart..token.area.end), obj
    ) on start..token.area.end to parser.pop()
else {
    val (expr, symbol) = handleExpression(token, parser, true)
    val newExpressions = expressions + expr
    when (symbol?.value) {
        is ClosedArrayBracketToken ->
            ArrayExpr(
                if (sections.isEmpty()) newExpressions else sections + (ArrayExpr(
                    newExpressions,
                    obj
                ) on lastStart..symbol.area.end), obj
            ) on
                    start..symbol.area.end to parser.pop()
        is CommaToken -> handleArray(parser.pop(), parser, start, obj, expressions + expr, lastStart, sections)
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, symbol?.area ?: parser.lastArea)
    }
}