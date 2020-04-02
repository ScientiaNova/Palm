package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.callVirtual
import com.scientianova.palm.registry.TypeRegistry
import com.scientianova.palm.registry.toType
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on
import com.scientianova.palm.util.pushing
import java.util.*

interface IOperationPart

interface IExpression : IOperationPart {
    override fun toString(): String

    fun handleForType(type: Class<*>, scope: Scope) = if (this is Object && type == null) {
        val palmType = TypeRegistry.getOrRegister(type)
        palmType.createInstance(values, scope)
    } else scope.cast(evaluate(scope), type)

    fun evaluate(scope: Scope = Scope.GLOBAL): Any?
}

typealias PositionedExpression = Positioned<IExpression>

data class Variable(val name: String) : IExpression {
    override fun evaluate(scope: Scope) = scope[name]
}

data class Num(val num: Number) : IExpression {
    override fun evaluate(scope: Scope) = num
}

data class Chr(val char: Char) : IExpression {
    override fun evaluate(scope: Scope) = char
}

data class Bool(val bool: Boolean) : IExpression {
    override fun evaluate(scope: Scope) = bool
}

object Null : IExpression {
    override fun toString() = "Null"
    override fun evaluate(scope: Scope): Nothing? = null
}

data class Lis(val expressions: List<IExpression> = emptyList()) : IExpression {
    constructor(expr: IExpression) : this(listOf(expr))

    override fun evaluate(scope: Scope) = expressions.map { it.evaluate(scope) }
}

data class ListComprehension(
    val expression: IExpression,
    val name: String,
    val collection: IExpression,
    val filter: IExpression? = null
) : IExpression {
    override fun evaluate(scope: Scope) = mutableListOf<Any?>().apply { evaluate(Scope(parent = scope), this) }

    fun evaluate(scope: Scope, result: MutableList<Any?>) {
        val collection = collection.evaluate(scope)
        if (expression is ListComprehension)
            for (thing in scope.getIterator(collection)) {
                scope[name] = thing
                if (filter == null || filter.evaluate(scope) == true)
                    expression.evaluate(scope, result)
            }
        else
            for (thing in scope.getIterator(collection)) {
                scope[name] = thing
                if (filter == null || filter.evaluate(scope) == true)
                    result.add(expression.evaluate(scope))
            }
    }
}

data class Dict(val values: Map<IExpression, IExpression> = emptyMap()) : IExpression {
    override fun evaluate(scope: Scope) =
        values.map { it.key.evaluate(scope) to it.value.evaluate(scope) }.toMap()
}

data class DictComprehension(
    val keyExpr: IExpression,
    val valueExpr: IExpression,
    val name: String,
    val collection: IExpression,
    val filter: IExpression? = null
) : IExpression {
    override fun evaluate(scope: Scope): Map<Any?, Any?> {
        val newScope = Scope(parent = scope)
        val collection = collection.evaluate(newScope)
        val result = mutableMapOf<Any?, Any?>()
        for (thing in scope.getIterator(collection)) {
            newScope[name] = thing
            if (filter == null || filter.evaluate(newScope) == true)
                result[keyExpr.evaluate(newScope)] = valueExpr.evaluate(newScope)
        }
        return result
    }
}

sealed class StrPart
data class StrStringPart(val string: String) : StrPart()
data class StrExpressionPart(val expr: IExpression) : StrPart()
data class Str(val parts: List<StrPart>) : IExpression {
    override fun evaluate(scope: Scope) = parts.joinToString("") {
        when (it) {
            is StrStringPart -> it.string
            is StrExpressionPart -> it.expr.evaluate(scope).toString()
        }
    }
}

data class Object(val values: Map<String, IExpression> = emptyMap(), val type: List<String>? = null) : IExpression {
    override fun evaluate(scope: Scope) =
        type?.toType()?.createInstance(values, scope) ?: error("Typeless object")
}

data class VirtualCall(
    val expr: IExpression,
    val name: String,
    val args: List<IExpression> = emptyList()
) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope)
        return value.callVirtual(name, scope, args.map { it.evaluate(scope) })
    }
}

data class SafeVirtualCall(
    val expr: IExpression,
    val name: String,
    val args: List<IExpression> = emptyList()
) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope) ?: return null
        return value.callVirtual(name, scope, args.map { it.evaluate(scope) })
    }
}

data class StaticCall(
    val name: String,
    val path: List<String>,
    val args: List<IExpression> = emptyList()
) : IExpression {
    override fun evaluate(scope: Scope) = scope.callStatic(name, path, args.map { it.evaluate(scope) })
}

data class If(val condExpr: IExpression, val thenExpr: IExpression, val elseExpr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        if (condExpr.evaluate(scope) == true) thenExpr.evaluate(scope) else elseExpr.evaluate(scope)
}

data class Where(val expr: IExpression, val definitions: List<Pair<String, IExpression>>) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val newScope = Scope(parent = scope)
        definitions.forEach { (name, expr) ->
            newScope[name] = expr.evaluate(newScope)
        }
        return expr.evaluate(newScope)
    }
}

data class When(val branches: List<Pair<IExpression, IExpression>>, val elseBranch: IExpression?) : IExpression {
    override fun evaluate(scope: Scope) =
        branches.firstOrNull { it.first.evaluate(scope) == true }?.second?.evaluate(scope)
            ?: elseBranch?.evaluate(scope)
            ?: error("Non-exhaustive when statement")
}

sealed class Pattern
data class ExpressionPattern(val expression: IExpression) : Pattern()
data class TypePattern(val type: List<String>, val inverted: Boolean = false) : Pattern()
data class ContainingPattern(val collection: IExpression, val inverted: Boolean = false) : Pattern()
data class ComparisonPattern(val operator: ComparisonOperatorToken, val expression: IExpression) : Pattern()
typealias SwitchBranch = Pair<List<Pattern>, IExpression>

data class WhenSwitch(
    val expr: IExpression,
    val branches: List<SwitchBranch>,
    val elseBranch: IExpression?
) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val evaluated = expr.evaluate(scope)
        return branches.firstOrNull { branch ->
            branch.first.any { pattern ->
                when (pattern) {
                    is ExpressionPattern -> evaluated == pattern.expression.evaluate(scope)
                    is TypePattern -> pattern.inverted != scope.getType(pattern.type).clazz.isInstance(evaluated)
                    is ContainingPattern -> {
                        val collection = pattern.collection.evaluate(scope)
                        pattern.inverted != collection.callVirtual("contains", scope, evaluated)
                    }
                    is ComparisonPattern -> {
                        val second = pattern.expression.evaluate(scope)
                        pattern.operator.comparisonType.handle(
                            evaluated.callVirtual("compareTo", scope, second) as Int
                        )
                    }
                }
            }
        }?.second?.evaluate(scope) ?: elseBranch?.evaluate(scope) ?: error("Not exhaustive when statement")
    }
}

data class Yield(
    val variableName: String,
    val expr: IExpression,
    val collection: IExpression,
    val iteratedName: String,
    val filter: IExpression?
) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val newScope = Scope(parent = scope)
        val collection = collection.evaluate(newScope)
        for (thing in scope.getIterator(collection)) {
            newScope[iteratedName] = thing
            if (filter == null || filter.evaluate(newScope) == true)
                newScope[variableName] = expr.evaluate(newScope)
        }
        return newScope[variableName]
    }
}

fun handleExpression(
    parser: Parser,
    token: PositionedToken?,
    untilLineEnd: Boolean = false
): Pair<PositionedExpression, PositionedToken?> {
    val (first, op) = handleExpressionPart(parser, token)
    val expr: PositionedExpression
    val next: PositionedToken?
    if (op != null && op.value is BinaryOperatorToken && (!untilLineEnd || op.area.end.row == first.area.start.row)) {
        val res = handleBinOps(parser, op.value, Stack<Positioned<IOperationPart>>().pushing(first))
        expr = res.first
        next = res.second
    } else {
        expr = first
        next = op
    }
    return if (next?.value is WhereToken) {
        val bracket = parser.pop()
        if (bracket?.value is OpenCurlyBracketToken)
            handleWhere(parser, parser.pop(), expr.value, expr.area.start)
        else parser.error(MISSING_CURLY_BRACKET_AFTER_WHERE_ERROR, bracket?.area?.start ?: parser.lastPos)
    } else expr to next
}

fun handleBinOps(
    parser: Parser,
    op: BinaryOperatorToken,
    operandStack: Stack<Positioned<IOperationPart>>,
    operatorStack: Stack<BinaryOperatorToken> = Stack(),
    untilLineEnd: Boolean = true
): Pair<PositionedExpression, PositionedToken?> =
    if (operatorStack.isEmpty() || op.precedence > operatorStack.peek().precedence) {
        when (op) {
            is TernaryOperatorToken -> {
                val condition = operandStack.pop()
                handleTernary(parser, parser.pop(), condition.value as IExpression, condition.area.start)
            }
            is WalrusOperatorToken -> {
                val previous = operandStack.pop()
                val name = previous.value as? Variable
                    ?: parser.error(INVALID_VARIABLE_NAME_IN_WALRUS_ERROR, previous.area)
                val (value, next) = handleExpression(parser, parser.pop())
                operandStack.push(Walrus(name.name, value.value) on previous.area.start..value.area.end)
                emptyStacks(next, operandStack, operatorStack)
            }
            else -> {
                val (operand, next) =
                    if (op is TypeOperatorToken) handleType(parser, parser.pop())
                    else handleExpressionPart(parser, parser.pop())
                operatorStack.push(op)
                operandStack.push(operand)
                if (next != null && next.value is BinaryOperatorToken && (!untilLineEnd || next.area.end.row == operand.area.start.row)) {
                    if (op is ComparisonOperatorToken && next.value is ComparisonOperatorToken) {
                        operatorStack.push(AndToken)
                        operandStack.push(operand)
                    }
                    handleBinOps(parser, next.value, operandStack, operatorStack, untilLineEnd)
                } else emptyStacks(next, operandStack, operatorStack)
            }
        }
    } else {
        val second = operandStack.pop()
        val first = operandStack.pop()
        handleBinOps(
            parser, op, operandStack.pushing(
                operatorStack.pop().handleExpression(first.value, second.value) on first.area.start..second.area.end
            ), operatorStack, untilLineEnd
        )
    }

fun emptyStacks(
    next: PositionedToken?,
    operandStack: Stack<Positioned<IOperationPart>>,
    operatorStack: Stack<BinaryOperatorToken> = Stack()
): Pair<PositionedExpression, PositionedToken?> =
    if (operatorStack.isEmpty()) {
        val result = operandStack.pop()
        result.value as IExpression on result.area to next
    } else {
        val second = operandStack.pop()
        val first = operandStack.pop()
        emptyStacks(
            next, operandStack.pushing(
                operatorStack.pop().handleExpression(first.value, second.value) on first.area.start..second.area.end
            ), operatorStack
        )
    }

fun handleExpressionPart(
    parser: Parser,
    token: PositionedToken?,
    untilLineEnd: Boolean = false
): Pair<PositionedExpression, PositionedToken?> {
    val (expr, afterExpr) =
        if (token == null)
            parser.error(MISSING_EXPRESSION_ERROR, parser.lastPos)
        else when (token.value) {
            is NumberToken -> Num(token.value.number) on token to parser.pop()
            is CharToken -> Chr(token.value.char) on token to parser.pop()
            is BoolToken -> Bool(token.value.bool) on token to parser.pop()
            is NullToken -> Null on token to parser.pop()
            is IdentifierToken ->
                handleIdentifier(parser, parser.pop(), token.area.start, token.area.end, token.value.name)
            is OpenCurlyBracketToken ->
                handleObject(parser, parser.pop(), token.area.start)
            is OpenParenToken -> {
                val (expr, closedParen) = handleExpression(parser, parser.pop())
                if (closedParen?.value is ClosedParenToken) expr to parser.pop()
                else parser.error(UNCLOSED_PARENTHESIS_ERROR, closedParen?.area?.start ?: parser.lastPos)
            }
            is OpenSquareBracketToken -> {
                val first = parser.pop()
                when (first?.value) {
                    is ClosedSquareBracketToken -> Lis() on token.area.start..first.area.end to parser.pop()
                    is ColonToken -> {
                        val bracket = parser.pop()
                        if (bracket?.value is ClosedSquareBracketToken) Dict() on bracket.area to parser.pop()
                        else parser.error(INVALID_EMPTY_MAP_ERROR, bracket?.area ?: parser.lastArea)
                    }
                    else -> {
                        val (expr, next) = handleExpression(parser, first)
                        when (next?.value) {
                            is CommaToken ->
                                handleList(parser, parser.pop(), token.area.start, listOf(expr.value))
                            is SemicolonToken ->
                                handleList(parser, parser.pop(), token.area.start, emptyList(), listOf(Lis(expr.value)))
                            is ColonToken -> {
                                val (value, newNext) = handleExpression(parser, parser.pop())
                                when (newNext?.value) {
                                    is CommaToken -> handleDict(
                                        parser, parser.pop(), token.area.start, mapOf(expr.value to value.value)
                                    )
                                    is ForToken -> handleDictComprehension(
                                        parser, parser.pop(), expr.value, value.value, expr.area.start
                                    )
                                    else -> handleDict(
                                        parser, newNext, token.area.start, mapOf(expr.value to value.value)
                                    )
                                }
                            }
                            is ForToken -> handleListComprehension(parser, parser.pop(), expr.value, token.area.start)
                            else -> handleList(parser, next, token.area.start, listOf(expr.value))
                        }
                    }
                }
            }
            is YieldToken -> handleYield(parser, parser.pop(), token.area.start, untilLineEnd)
            is PureStringToken -> Str(listOf(StrStringPart(token.value.name))) on token to parser.pop()
            is StringTemplateToken -> Str(token.value.parts.map {
                when (it) {
                    is StringPart -> StrStringPart(it.string)
                    is TokensPart -> {
                        val (expr, nullToken) = handleExpression(parser.handle(it.tokens), it.tokens.poll())
                        if (nullToken != null) parser.error(INVALID_INTERPOLATION_ERROR, nullToken.area.start)
                        StrExpressionPart(expr.value)
                    }
                }
            }) on token to parser.pop()
            is UnaryOperatorToken -> {
                val (expr, next) = handleExpressionPart(parser, parser.pop())
                VirtualCall(expr.value, token.value.name) on token.area.start..expr.area.end to next
            }
            is IfToken -> handleIf(parser, parser.pop(), token.area.start, untilLineEnd)
            is WhenToken -> {
                val afterWhen = parser.pop()
                if (afterWhen?.value is OpenCurlyBracketToken) handleWhen(parser, parser.pop(), token.area.start)
                else {
                    val (expr, bracket) = handleExpression(parser, afterWhen)
                    if (bracket?.value is OpenCurlyBracketToken)
                        handleWhenSwitch(parser, parser.pop(), token.area.start, expr.value)
                    else parser.error(MISSING_CURLY_BRACKET_AFTER_WHEN_ERROR, bracket?.area?.start ?: parser.lastPos)
                }
            }
            else -> parser.error(INVALID_EXPRESSION_ERROR, token.area)
        }
    return handlePostfixOperations(parser, afterExpr, expr)
}

fun handlePostfixOperations(
    parser: Parser,
    token: PositionedToken?,
    expr: PositionedExpression
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is DotToken -> {
        val name = parser.pop()
        if (name == null || name.value !is IdentifierToken)
            parser.error(INVALID_PROPERTY_NAME_ERROR, name?.area ?: parser.lastArea)
        val next = parser.pop()
        if (next?.value is OpenParenToken) {
            val args = handleFunctionArguments(parser, parser.pop())
            handlePostfixOperations(
                parser, parser.pop(),
                VirtualCall(expr.value, name.value.name, args.first) on expr.area.start..args.second
            )
        } else handlePostfixOperations(
            parser, next, VirtualCall(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is SafeAccessToken -> {
        val name = parser.pop()
        if (name == null || name.value !is IdentifierToken)
            parser.error(INVALID_PROPERTY_NAME_ERROR, name?.area ?: parser.lastArea)
        handlePostfixOperations(
            parser, parser.pop(), SafeVirtualCall(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is OpenSquareBracketToken -> {
        val (getter, next) = handleGetter(parser, parser.pop(), expr.value, expr.area.start)
        handlePostfixOperations(parser, next, getter)
    }
    else -> expr to token
}

fun handleIdentifier(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    endPos: StringPos,
    current: String,
    path: List<String> = emptyList()
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is DoubleColonToken -> {
        val top = parser.pop()
        val name = (top.value as? IdentifierToken)?.name
            ?: parser.error(INVALID_PROPERTY_NAME_ERROR, top?.area ?: parser.lastArea)
        handleIdentifier(parser, parser.pop(), startPos, top.area.end, name, path + current)
    }
    is OpenCurlyBracketToken -> handleTypedObject(parser, parser.pop(), startPos, path)
    is OpenParenToken -> {
        val args = handleFunctionArguments(parser, parser.pop())
        StaticCall(current, path, args.first) on startPos..args.second to parser.pop()
    }
    else -> (if (path.isEmpty()) Variable(current) else StaticCall(current, path)) on startPos..endPos to token
}

fun handleFunctionArguments(
    parser: Parser,
    token: PositionedToken?,
    args: List<IExpression> = emptyList()
): Pair<List<IExpression>, StringPos> = when (token?.value) {
    null -> parser.error(UNCLOSED_PARENTHESIS_ERROR, parser.lastPos)
    is ClosedParenToken -> args to token.area.end
    else -> {
        val (expr, next) = handleExpression(parser, token)
        when (next?.value) {
            is ClosedParenToken -> args + expr.value to next.area.end
            is CommaToken -> handleFunctionArguments(parser, parser.pop(), args + expr.value)
            else -> parser.error(UNCLOSED_PARENTHESIS_ERROR, next?.area?.start ?: parser.lastPos)
        }
    }
}

fun handleList(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    values: List<IExpression>,
    parsers: List<Lis> = emptyList()
): Pair<Positioned<Lis>, PositionedToken?> = when {
    token == null -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, parser.lastPos)
    token.value is ClosedSquareBracketToken ->
        (if (parsers.isEmpty()) Lis(values) else Lis(parsers + Lis(values))) on startPos..token.area.end to parser.pop()
    else -> {
        val (expr, next) = handleExpression(parser, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                (if (parsers.isEmpty()) Lis(values + expr.value) else Lis(parsers + Lis(values + expr.value))) on
                        startPos..token.area.end to parser.pop()
            is CommaToken ->
                handleList(parser, parser.pop(), startPos, values + expr.value, parsers)
            is SemicolonToken ->
                handleList(parser, parser.pop(), startPos, emptyList(), parsers + Lis(values + expr.value))
            else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, next?.area?.start ?: parser.lastPos)
        }
    }
}

fun handleDict(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    values: Map<IExpression, IExpression>
): Pair<Positioned<Dict>, PositionedToken?> = when {
    token == null -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, parser.lastPos)
    token.value is ClosedSquareBracketToken -> Dict(values) on startPos..token.area.end to parser.pop()
    else -> {
        val (key, colon) = handleExpression(parser, token)
        if (colon?.value !is ColonToken) parser.error(
            MISSING_COLON_IN_MAP_ERROR,
            colon?.area?.start ?: parser.lastPos
        )
        val (value, next) = handleExpression(parser, parser.pop())
        when (next?.value) {
            is ClosedSquareBracketToken ->
                Dict(values + (key.value to value.value)) on startPos..token.area.end to parser.pop()
            is CommaToken ->
                handleDict(parser, parser.pop(), startPos, values + (key.value to value.value))
            else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, next?.area?.start ?: parser.lastPos)
        }
    }
}

fun handleListComprehension(
    parser: Parser,
    token: PositionedToken?,
    expr: IExpression,
    startPos: StringPos
): Pair<PositionedExpression, PositionedToken?> {
    if (token == null || token.value !is IdentifierToken)
        parser.error(INVALID_VARIABLE_NAME_IN_LIST_COMPREHENSION_ERROR, token?.area ?: parser.lastArea)
    val name = token.value.name
    val inToken = parser.pop()
    if (inToken?.value !is InToken) parser.error(MISSING_IN_IN_LIST_ERROR, inToken?.area ?: parser.lastArea)
    val (collection, afterCollection) = handleExpression(parser, parser.pop())
    return when (afterCollection?.value) {
        is ClosedSquareBracketToken ->
            ListComprehension(expr, name, collection.value) on startPos..afterCollection.area.end to parser.pop()
        is ForToken ->
            handleListComprehension(parser, parser.pop(), ListComprehension(expr, name, collection.value), startPos)
        is IfToken -> {
            val (filter, afterFilter) = handleExpression(parser, parser.pop())
            when (afterFilter?.value) {
                is ClosedSquareBracketToken -> ListComprehension(expr, name, collection.value, filter.value) on
                        startPos..afterCollection.area.end to parser.pop()
                is ForToken -> handleListComprehension(
                    parser, parser.pop(), ListComprehension(expr, name, collection.value, filter.value), startPos
                )
                else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterFilter?.area?.start ?: parser.lastPos)
            }
        }
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterCollection?.area?.start ?: parser.lastPos)
    }
}

fun handleDictComprehension(
    parser: Parser,
    token: PositionedToken?,
    keyExpr: IExpression,
    valueExpr: IExpression,
    startPos: StringPos
): Pair<PositionedExpression, PositionedToken?> {
    if (token == null || token.value !is IdentifierToken)
        parser.error(INVALID_VARIABLE_NAME_IN_MAP_COMPREHENSION_ERROR, token?.area ?: parser.lastArea)
    val name = token.value.name
    val inToken = parser.pop()
    if (inToken?.value !is InToken) parser.error(MISSING_IN_IN_MAP_ERROR, inToken?.area ?: parser.lastArea)
    val (collection, afterCollection) = handleExpression(parser, parser.pop())
    return when (afterCollection?.value) {
        is ClosedSquareBracketToken -> DictComprehension(keyExpr, valueExpr, name, collection.value) on
                startPos..afterCollection.area.end to parser.pop()
        is IfToken -> {
            val (filter, afterFilter) = handleExpression(parser, parser.pop())
            if (afterFilter?.value is ClosedSquareBracketToken)
                DictComprehension(keyExpr, valueExpr, name, collection.value, filter.value) on
                        startPos..afterCollection.area.end to parser.pop()
            else parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterFilter?.area?.start ?: parser.lastPos)
        }
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterCollection?.area?.start ?: parser.lastPos)
    }
}

fun handleObject(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> =
    if (token == null) parser.error(UNCLOSED_OBJECT_ERROR, parser.lastPos) else when (token.value) {
        is ClosedCurlyBracketToken -> Object(values) on startPos..token.area.end to parser.pop()
        is IKeyToken -> parser.pop().let { assignToken ->
            val (expr, next) = when (assignToken?.value) {
                is AssignmentToken -> {
                    val exprStart = parser.pop()
                    if (token.value.name == "type" && exprStart != null && exprStart.value is PureStringToken) {
                        val afterType = parser.pop()
                        return handleTypedObject(
                            parser, if (afterType?.value is SeparatorToken) parser.pop() else afterType, startPos,
                            exprStart.value.name.split("([.:])".toRegex())
                        )
                    }
                    handleExpression(parser, exprStart, true)
                }
                is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
                else -> parser.error(
                    MISSING_COLON_OR_EQUALS_IN_OBJECT_ERROR,
                    assignToken?.area?.start ?: parser.lastPos
                )
            }
            when (next?.value) {
                is ClosedCurlyBracketToken ->
                    Object(values + (token.value.name to expr.value)) on
                            startPos..token.area.end to parser.pop()
                is SeparatorToken ->
                    handleObject(parser, parser.pop(), startPos, values + (token.value.name to expr.value))
                else -> handleObject(parser, next, startPos, values + (token.value.name to expr.value))
            }
        }
        else -> parser.error(INVALID_KEY_NAME_ERROR, token.area)
    }

fun handleTypedObject(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    type: List<String>,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> =
    if (token == null) parser.error(UNCLOSED_OBJECT_ERROR, parser.lastPos) else when (token.value) {
        is ClosedCurlyBracketToken -> Object(values, type) on startPos..token.area.end to parser.pop()
        is IKeyToken -> parser.pop().let { assignToken ->
            val (expr, next) = when (assignToken?.value) {
                is AssignmentToken -> handleExpression(parser, parser.pop())
                is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
                else -> parser.error(
                    MISSING_COLON_OR_EQUALS_IN_OBJECT_ERROR,
                    assignToken?.area?.start ?: parser.lastPos
                )
            }
            when (next?.value) {
                is ClosedCurlyBracketToken ->
                    Object(values + (token.value.name to expr.value), type) on
                            startPos..token.area.end to parser.pop()
                is SeparatorToken ->
                    handleTypedObject(
                        parser,
                        parser.pop(),
                        startPos,
                        type,
                        values + (token.value.name to expr.value)
                    )
                else -> handleTypedObject(parser, next, startPos, type, values + (token.value.name to expr.value))
            }
        }
        else -> parser.error(INVALID_KEY_NAME_ERROR, token.area)
    }

fun handleIf(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    untilLineEnd: Boolean = false
): Pair<Positioned<If>, PositionedToken?> {
    val (cond, thenToken) = handleExpression(parser, token)
    val (thenExpr, elseToken) = handleExpression(
        parser,
        if (thenToken?.value is ThenToken) parser.pop() else thenToken
    )
    if (elseToken?.value !is ElseToken) parser.error(MISSING_ELSE_BRANCH, elseToken?.area?.start ?: parser.lastPos)
    val (elseExpr, next) = handleExpression(parser, parser.pop(), untilLineEnd)
    return If(cond.value, thenExpr.value, elseExpr.value) on startPos..elseExpr.area.end to next
}

fun handleTernary(
    parser: Parser,
    token: PositionedToken?,
    condExpr: IExpression,
    startPos: StringPos,
    untilLineEnd: Boolean = false
): Pair<Positioned<If>, PositionedToken?> {
    val (thenExpr, colon) = handleExpression(parser, token)
    if (colon?.value !is ColonToken) parser.error(
        MISSING_COLON_IN_TERNARY_ERROR,
        colon?.area?.start ?: parser.lastPos
    )
    val (elseExpr, next) = handleExpression(parser, parser.pop(), untilLineEnd)
    return If(condExpr, thenExpr.value, elseExpr.value) on startPos..elseExpr.area.end to next
}

fun handleGetter(
    parser: Parser,
    token: PositionedToken?,
    expr: IExpression,
    startPos: StringPos,
    params: List<IExpression> = emptyList()
): Pair<PositionedExpression, PositionedToken?> = when {
    token == null -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, parser.lastPos)
    token.value is ClosedSquareBracketToken ->
        VirtualCall(expr, "get", params) on startPos..token.area.end to parser.pop()
    else -> {
        val (currentExpr, next) = handleExpression(parser, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                VirtualCall(expr, "get", params + currentExpr.value) on startPos..next.area.end to parser.pop()
            is CommaToken ->
                handleGetter(parser, parser.pop(), expr, startPos, params + currentExpr.value)
            else -> handleGetter(parser, next, expr, startPos, params + currentExpr.value)
        }
    }
}

fun handleWhere(
    parser: Parser,
    token: PositionedToken?,
    expression: IExpression,
    startPos: StringPos,
    values: List<Pair<String, IExpression>> = emptyList()
): Pair<Positioned<Where>, PositionedToken?> =
    if (token == null) parser.error(UNCLOSED_WHERE_ERROR, parser.lastPos) else when (token.value) {
        is ClosedCurlyBracketToken -> Where(expression, values) on startPos..token.area.end to parser.pop()
        is IKeyToken -> parser.pop().let { assignToken ->
            val (expr, next) = when (assignToken?.value) {
                is AssignmentToken ->
                    handleExpression(parser, parser.pop())
                is OpenCurlyBracketToken ->
                    handleObject(parser, parser.pop(), assignToken.area.start)
                else ->
                    parser.error(MISSING_COLON_OR_EQUALS_IN_WHERE_ERROR, assignToken?.area?.start ?: parser.lastPos)
            }
            when (next?.value) {
                is ClosedCurlyBracketToken ->
                    Where(expression, values + (token.value.name to expr.value)) on
                            startPos..token.area.end to parser.pop()
                is SeparatorToken ->
                    handleWhere(parser, parser.pop(), expression, startPos, values + (token.value.name to expr.value))
                else ->
                    handleWhere(parser, next, expression, startPos, values + (token.value.name to expr.value))
            }
        }
        else -> parser.error(INVALID_VARIABLE_NAME_IN_WHERE_ERROR, token.area)
    }

fun handleWhen(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    branches: List<Pair<IExpression, IExpression>> = emptyList(),
    elseExpr: IExpression? = null
): Pair<Positioned<When>, PositionedToken?> = when (token?.value) {
    is ClosedCurlyBracketToken -> When(branches, elseExpr) on startPos..token.area.end to parser.pop()
    is ElseToken -> {
        val arrow = parser.pop()
        if (arrow?.value !is ArrowToken) parser.error(MISSING_ARROW_ERROR, arrow?.area ?: parser.lastArea)
        val (expr, next) = handleExpression(parser, parser.pop())
        if (elseExpr == null) handleWhen(parser, next, startPos, branches, expr.value)
        else handleWhen(parser, next, startPos, branches, elseExpr)
    }
    else -> {
        val (condition, arrow) = handleExpression(parser, token)
        if (arrow?.value !is ArrowToken) parser.error(MISSING_ARROW_ERROR, arrow?.area ?: parser.lastArea)
        val (expr, next) = handleExpression(parser, parser.pop())
        if (elseExpr == null) handleWhen(parser, next, startPos, branches + (condition.value to expr.value))
        else handleWhen(parser, next, startPos, branches, elseExpr)
    }
}

fun handleWhenSwitch(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    value: IExpression,
    branches: List<SwitchBranch> = emptyList(),
    elseExpr: IExpression? = null
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is ClosedCurlyBracketToken -> WhenSwitch(value, branches, elseExpr) on startPos..token.area.end to parser.pop()
    is ElseToken -> {
        val arrow = parser.pop()
        if (arrow?.value !is ArrowToken) parser.error(MISSING_ARROW_ERROR, arrow?.area ?: parser.lastArea)
        val (expr, next) = handleExpression(parser, parser.pop(), true)
        if (elseExpr == null) handleWhenSwitch(parser, next, startPos, value, branches, expr.value)
        else handleWhenSwitch(parser, next, startPos, value, branches, elseExpr)
    }
    else -> {
        val (branch, next) = handleSwitchBranch(parser, token)
        if (elseExpr == null) handleWhenSwitch(parser, next, startPos, value, branches + branch, elseExpr)
        else handleWhenSwitch(parser, next, startPos, value, branches)
    }
}

fun handleSwitchBranch(
    parser: Parser,
    token: PositionedToken?,
    patterns: List<Pattern> = emptyList()
): Pair<SwitchBranch, PositionedToken?> {
    if (token == null) parser.error(UNCLOSED_WHEN_ERROR, parser.lastPos)
    val (pattern, separator) = when (token.value) {
        is TypeOperatorToken -> {
            val (type, next) = handleType(parser, parser.pop())
            TypePattern(type.value.path, token.value is IsNotToken) to next
        }
        is ContainingOperatorToken -> {
            val (expr, next) = handleExpression(parser, parser.pop())
            ContainingPattern(expr.value, token.value is NotInToken) to next
        }
        is ComparisonOperatorToken -> {
            val (expr, next) = handleExpression(parser, parser.pop())
            ComparisonPattern(token.value, expr.value) to next
        }
        else -> {
            val (expr, next) = handleExpression(parser, token)
            ExpressionPattern(expr.value) to next
        }
    }
    return when (separator?.value) {
        is CommaToken -> handleSwitchBranch(parser, parser.pop(), patterns + pattern)
        is ArrowToken -> {
            val (res, next) = handleExpression(parser, parser.pop(), true)
            patterns + pattern to res.value to next
        }
        else -> parser.error(MISSING_ARROW_ERROR, separator?.area ?: parser.lastArea)
    }
}

fun handleYield(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    untilLineEnd: Boolean = false
): Pair<PositionedExpression, PositionedToken?> {
    val resultName = (token?.value as? IdentifierToken)?.name
        ?: parser.error(INVALID_VARIABLE_NAME_IN_YIELD_ERROR, token?.area ?: parser.lastArea)
    val walrus = parser.pop()
    if (walrus?.value !is WalrusOperatorToken)
        parser.error(MISSING_WALRUS_IN_YIELD_ERROR, walrus?.area?.start ?: parser.lastPos)
    val (expression, forToken) = handleExpression(parser, parser.pop())
    if (forToken?.value !is ForToken)
        parser.error(MISSING_FOR_IN_YIELD_ERROR, forToken?.area?.start ?: parser.lastPos)
    val iteratingVariable = parser.pop()
    val iteratingName = (iteratingVariable?.value as? IdentifierToken)?.name
        ?: parser.error(INVALID_VARIABLE_NAME_IN_YIELD_ERROR, iteratingVariable?.area ?: parser.lastArea)
    val inToken = parser.pop()
    if (inToken?.value !is InToken)
        parser.error(MISSING_IN_IN_YIELD_ERROR, inToken?.area?.start ?: parser.lastPos)
    val (collection, afterCollection) = handleExpression(parser, parser.pop(), untilLineEnd)
    return if (afterCollection?.value is IfToken) {
        val (filter, next) = handleExpression(parser, parser.pop(), untilLineEnd)
        Yield(resultName, expression.value, collection.value, iteratingName, filter.value) on
                startPos..filter.area.end to next
    } else Yield(resultName, expression.value, collection.value, iteratingName, null) on
            startPos..collection.area.end to afterCollection
}