package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_INTERPOLATION_ERROR
import com.scientianova.palm.errors.UNCLOSED_PARENTHESIS_ERROR
import com.scientianova.palm.errors.UNCLOSED_SQUARE_BRACKET_ERROR
import com.scientianova.palm.evaluator.Scope
import com.scientianova.palm.evaluator.cast
import com.scientianova.palm.evaluator.palmType
import com.scientianova.palm.registry.TypeName
import com.scientianova.palm.registry.TypeRegistry
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.*
import java.util.*

interface IOperationPart

interface IExpression : IOperationPart {
    override fun toString(): String

    fun handleForType(type: Class<*>, scope: Scope) = if (this is Object && type == null) {
        val palmType = TypeRegistry.getOrRegister(type)
        palmType.createInstance(values, scope)
    } else evaluate(scope).cast(type)

    fun evaluate(scope: Scope = Scope.GLOBAL): Any?

    fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean): List<T> {
        if (type.isInstance(this)) {
            val casted = type.cast(this)
            if (predicate(casted)) return listOf(casted)
        }
        return emptyList()
    }
}

typealias PositionedExpression = Positioned<IExpression>

data class Constant(val name: String) : IExpression {
    override fun evaluate(scope: Scope) =
        if (name in scope) scope[name] else error("Couldn't find a constant called $name in the current scope")
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

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expressions.flatMap { it.find(type, predicate) }
}

data class Dict(val values: Map<IExpression, IExpression>) : IExpression {
    override fun evaluate(scope: Scope) =
        values.map { it.key.evaluate(scope) to it.value.evaluate(scope) }.toMap()

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + values.flatMap { it.key.find(type, predicate) + it.value.find(type, predicate) }
}

data class Comprehension(
    val expression: IExpression,
    val name: String,
    val collection: IExpression,
    val filter: IExpression? = null
) : IExpression {
    override fun evaluate(scope: Scope) = mutableListOf<Any?>().apply { evaluate(scope, this) }

    fun evaluate(scope: Scope, result: MutableList<Any?>) {
        val collection = collection.evaluate(scope)
        if (expression is Comprehension)
            for (thing in collection.palmType.iterator(collection)) {
                val newScope = Scope(mutableMapOf(name to thing), scope)
                if (filter == null || filter.evaluate(newScope) == true)
                    expression.evaluate(newScope, result)
            }
        else
            for (thing in collection.palmType.iterator(collection)) {
                val newScope = Scope(mutableMapOf(name to thing), scope)
                if (filter == null || filter.evaluate(newScope) == true)
                    result.add(expression.evaluate(newScope))
            }
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expression.find(type, predicate) +
                collection.find(type, predicate) + (filter?.find(type, predicate) ?: emptyList())
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

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + parts.filterIsInstance<StrExpressionPart>()
            .flatMap { it.expr.find(type, predicate) }
}

data class Object(val values: Map<String, IExpression> = emptyMap(), val type: TypeName? = null) : IExpression {
    override fun evaluate(scope: Scope) =
        type?.toType()?.createInstance(values, scope) ?: error("Typeless object")

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + values.values.flatMap { it.find(type, predicate) }
}

data class ValAccess(val expr: IExpression, val field: String) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope)
        return value.palmType.get(value, field)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
}

data class SafeValAccess(val expr: IExpression, val field: String) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val value = expr.evaluate(scope) ?: return null
        return value.palmType.get(value, field)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + expr.find(type, predicate)
}

data class If(val condExpr: IExpression, val thenExpr: IExpression, val elseExpr: IExpression) : IExpression {
    override fun evaluate(scope: Scope) =
        if (condExpr.evaluate(scope) == true) thenExpr.evaluate(scope) else elseExpr.evaluate(scope)

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) =
        super.find(type, predicate) + condExpr.find(type, predicate) +
                thenExpr.find(type, predicate) + elseExpr.find(type, predicate)
}

data class Where(val expr: IExpression, val definitions: List<Pair<String, IExpression>>) : IExpression {
    override fun evaluate(scope: Scope): Any? {
        val newScope = Scope(parent = scope)
        definitions.forEach { (name, expr) ->
            newScope[name] = expr.evaluate(newScope)
        }
        return expr.evaluate(newScope)
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) = super.find(type, predicate) +
            expr.find(type, predicate) + definitions.flatMap { it.second.find(type, predicate) }
}

data class When(val branches: List<Pair<IExpression, IExpression>>, val elseBranch: IExpression?) : IExpression {
    override fun evaluate(scope: Scope) =
        branches.firstOrNull { it.first.evaluate(scope) == true }?.second?.evaluate(scope)
            ?: elseBranch?.evaluate(scope)
            ?: error("Not exhaustive when statement")

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) = super.find(type, predicate) +
            branches.flatMap { it.first.find(type, predicate) + it.second.find(type, predicate) } +
            (elseBranch?.find(type, predicate) ?: emptyList())
}

sealed class Pattern
data class ExpressionPattern(val expression: IExpression) : Pattern()
data class TypePattern(val type: Class<*>, val inverted: Boolean = false) : Pattern()
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
                    is TypePattern -> pattern.inverted != pattern.type.isInstance(evaluated)
                    is ContainingPattern -> {
                        val collection = pattern.collection.evaluate(scope)
                        pattern.inverted != collection.palmType.execute(Contains, collection, evaluated)
                    }
                    is ComparisonPattern -> {
                        val second = pattern.expression.evaluate(scope)
                        pattern.operator.comparisonType.handle(
                            evaluated.palmType.execute(CompareTo, evaluated, second) as Int
                        )
                    }
                }
            }
        }?.second?.evaluate(scope) ?: elseBranch?.evaluate(scope) ?: error("Not exhaustive when statement")
    }

    override fun <T : IExpression> find(type: Class<out T>, predicate: (T) -> Boolean) = super.find(type, predicate) +
            branches.flatMap {
                it.first.flatMap { pattern ->
                    when (pattern) {
                        is ExpressionPattern -> pattern.expression.find(type, predicate)
                        is TypePattern -> emptyList()
                        is ContainingPattern -> pattern.collection.find(type, predicate)
                        is ComparisonPattern -> pattern.expression.find(type, predicate)
                    }
                } + it.second.find(type, predicate)
            } + (elseBranch?.find(type, predicate) ?: emptyList())
}


fun handleExpression(parser: Parser, token: PositionedToken?): Pair<PositionedExpression, PositionedToken?> {
    val (first, op) = handleExpressionPart(parser, token)
    val expr: PositionedExpression
    val next: PositionedToken?
    if (op != null && op.value is OperatorToken && (op.value !is IUnaryOperatorToken || op.area.end.row == first.area.start.row)) {
        val res = handleBinOps(parser, op.value, first.area.start, Stack<IOperationPart>().pushing(first.value))
        expr = res.first
        next = res.second
    } else {
        expr = first
        next = op
    }
    return if (next?.value is WhereToken) {
        if (parser.pop()?.value is OpenCurlyBracketToken)
            handleWhere(parser, parser.pop(), expr.value, expr.area.start)
        else error("Missing open curly bracket after where")
    } else expr to next
}

fun handleBinOps(
    parser: Parser,
    op: OperatorToken,
    startPos: StringPos,
    operandStack: Stack<IOperationPart>,
    operatorStack: Stack<OperatorToken> = Stack()
): Pair<PositionedExpression, PositionedToken?> =
    if (operatorStack.isEmpty() || op.precedence > operatorStack.peek().precedence) {
        if (op == TernaryOperatorToken)
            handleTernary(parser, parser.pop(), operandStack.pop() as IExpression, startPos)
        else {
            val (operand, next) =
                if (op is TypeOperatorToken) handleType(parser, parser.pop())
                else handleExpressionPart(parser, parser.pop())
            operatorStack.push(op)
            operandStack.push(operand.value)
            if (next != null && next.value is OperatorToken && (next.value !is IUnaryOperatorToken || next.area.end.row == operand.area.start.row)) {
                if (op is ComparisonOperatorToken && next.value is ComparisonOperatorToken) {
                    operatorStack.push(AndToken)
                    operandStack.push(operand.value)
                }
                handleBinOps(parser, next.value, startPos, operandStack, operatorStack)
            } else emptyStacks(next, startPos..operand.area.end, operandStack, operatorStack)
        }
    } else {
        val second = operandStack.pop()
        handleBinOps(
            parser,
            op,
            startPos,
            operandStack.pushing(operatorStack.pop().handleExpression(operandStack.pop(), second)),
            operatorStack
        )
    }

fun emptyStacks(
    next: PositionedToken?,
    area: StringArea,
    operandStack: Stack<IOperationPart>,
    operatorStack: Stack<OperatorToken> = Stack()
): Pair<PositionedExpression, PositionedToken?> =
    if (operatorStack.isEmpty()) operandStack.pop() as IExpression on area to next else {
        val second = operandStack.pop()
        emptyStacks(
            next, area, operandStack.pushing(operatorStack.pop().handleExpression(operandStack.pop(), second)),
            operatorStack
        )
    }

fun handleExpressionPart(parser: Parser, token: PositionedToken?): Pair<PositionedExpression, PositionedToken?> {
    val (expr, afterExpr) = if (token == null) error("Missing expression part") else when (token.value) {
        is NumberToken -> Num(token.value.number) on token to parser.pop()
        is CharToken -> Chr(token.value.char) on token to parser.pop()
        is BoolToken -> Bool(token.value.bool) on token to parser.pop()
        is NullToken -> Null on token to parser.pop()
        is IdentifierToken ->
            handleIdentifierSequence(parser, parser.pop(), token.area.start, listOf(token.value.name))
        is OpenCurlyBracketToken ->
            handleObject(parser, parser.pop(), token.area.start)
        is OpenParenToken -> {
            val (expr, closedParen) = handleExpression(parser, parser.pop())
            if (closedParen?.value is ClosedParenToken) expr to parser.pop()
            else parser.error(UNCLOSED_PARENTHESIS_ERROR, closedParen?.area?.start ?: parser.lastPos)
        }
        is OpenSquareBracketToken -> {
            val first = parser.pop()
            if (first?.value is ClosedSquareBracketToken)
                Lis() on token.area.start..first.area.end to parser.pop()
            else {
                val (expr, next) = handleExpression(parser, first)
                when (next?.value) {
                    is CommaToken ->
                        handleSecondInList(parser, parser.pop(), token.area.start, expr.value)
                    is SemicolonToken ->
                        handleList(parser, parser.pop(), token.area.start, emptyList(), listOf(Lis(expr.value)))
                    is ColonToken -> {
                        val (value, newNext) = handleExpression(parser, parser.pop())
                        if (newNext?.value is CommaToken)
                            handleDict(parser, parser.pop(), token.area.start, mapOf(expr.value to value.value))
                        else handleDict(parser, newNext, token.area.start, mapOf(expr.value to value.value))
                    }
                    is DoubleDotToken -> {
                        val (last, closedBracket) = handleExpression(parser, parser.pop())
                        if (closedBracket?.value is ClosedSquareBracketToken)
                            MultiOp(RangeTo, expr.value, listOf(last.value)) on
                                    token.area.start..closedBracket.area.end to parser.pop()
                        else parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, closedBracket?.area?.start ?: parser.lastPos)
                    }
                    is ForToken -> handleComprehension(parser, parser.pop(), expr.value, token.area.start)
                    else -> handleSecondInList(parser, next, token.area.start, expr.value)
                }
            }
        }
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
        is IUnaryOperatorToken -> {
            val (expr, next) = handleExpressionPart(parser, parser.pop())
            UnaryOp(token.value.unaryOp, expr.value) on token.area.start..expr.area.end to next
        }
        is IfToken -> handleIf(parser, parser.pop(), token.area.start)
        is WhenToken -> {
            val afterWhen = parser.pop()
            if (afterWhen?.value is OpenCurlyBracketToken) handleWhen(parser, parser.pop(), token.area.start)
            else {
                val (expr, bracket) = handleExpression(parser, parser.pop())
                if (bracket?.value is OpenCurlyBracketToken)
                    handleWhenSwitch(parser, parser.pop(), token.area.start, expr.value)
                else error("Missing curly bracket after when")
            }
        }
        else -> error("Invalid expression part")
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
        if (name == null || name.value !is IdentifierToken) error("Invalid field name")
        handlePostfixOperations(
            parser, parser.pop(), ValAccess(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is SafeAccessToken -> {
        val name = parser.pop()
        if (name == null || name.value !is IdentifierToken) error("Invalid field name")
        handlePostfixOperations(
            parser, parser.pop(), SafeValAccess(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is OpenSquareBracketToken -> {
        val (getter, next) = handleGetter(parser, parser.pop(), expr.value, expr.area.start)
        handlePostfixOperations(parser, next, getter)
    }
    else -> expr to token
}

fun handleIdentifierSequence(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    identifiers: List<String>
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is DotToken -> {
        val top = parser.pop()?.value as? IdentifierToken ?: error("Invalid field name")
        handleIdentifierSequence(parser, parser.pop(), startPos, identifiers + top.name)
    }
    is OpenCurlyBracketToken ->
        handleTypedObject(
            parser,
            parser.pop(),
            startPos,
            TypeName(identifiers.dropLast(1).joinToString(".") { it }, identifiers.last())
        )
    else -> identifiers.drop(1).fold(Constant(identifiers.first()) as IExpression) { acc, s -> ValAccess(acc, s) } on
            startPos to token
}

fun handleSecondInList(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    first: IExpression
): Pair<PositionedExpression, PositionedToken?> = when {
    token == null -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, parser.lastPos)
    token.value is ClosedSquareBracketToken -> Lis(first) on startPos..token.area.end to parser.pop()
    else -> {
        val (expr, next) = handleExpression(parser, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                Lis(listOf(first, expr.value)) on startPos..token.area.end to parser.pop()
            is CommaToken ->
                handleList(parser, parser.pop(), startPos, listOf(first, expr.value))
            is SemicolonToken ->
                handleList(parser, parser.pop(), startPos, emptyList(), listOf(Lis(listOf(first, expr.value))))
            is DoubleDotToken -> {
                val (last, closedBracket) = handleExpression(parser, parser.pop())
                if (closedBracket?.value is ClosedSquareBracketToken)
                    MultiOp(RangeTo, first, listOf(expr.value, last.value)) on startPos..closedBracket.area.end to
                            parser.pop()
                else parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, closedBracket?.area?.start ?: parser.lastPos)
            }
            else -> handleList(parser, next, startPos, listOf(first, expr.value))
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
            else -> handleList(parser, next, startPos, values + expr.value, parsers)
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
        if (colon?.value !is ColonToken) error("Missing colon in dict")
        val (value, next) = handleExpression(parser, parser.pop())
        when (next?.value) {
            is ClosedSquareBracketToken ->
                Dict(values + (key.value to value.value)) on startPos..token.area.end to parser.pop()
            is CommaToken ->
                handleDict(parser, parser.pop(), startPos, values + (key.value to value.value))
            else -> handleDict(parser, colon, startPos, values + (key.value to value.value))
        }
    }
}

fun handleComprehension(
    parser: Parser,
    token: PositionedToken?,
    expr: IExpression,
    startPos: StringPos
): Pair<PositionedExpression, PositionedToken?> {
    if (token == null || token.value !is IdentifierToken)
        error("Invalid variable name in parser comprehension")
    val name = token.value.name
    if (parser.pop()?.value !is InToken) error("Missing `in` in parser comprehension")
    val (collection, afterCollection) = handleExpression(parser, parser.pop())
    return when (afterCollection?.value) {
        is ClosedSquareBracketToken ->
            Comprehension(expr, name, collection.value) on startPos..afterCollection.area.end to parser.pop()
        is ForToken -> {
            val (nested, next) = handleComprehension(parser, parser.pop(), expr, startPos)
            Comprehension(nested.value, name, collection.value) on startPos..afterCollection.area.end to next
        }
        is IfToken -> {
            val (filter, afterFilter) = handleExpression(parser, parser.pop())
            when (afterFilter?.value) {
                is ClosedSquareBracketToken -> Comprehension(expr, name, collection.value, filter.value) on
                        startPos..afterCollection.area.end to parser.pop()
                is ForToken -> {
                    val (nested, next) = handleComprehension(parser, parser.pop(), expr, startPos)
                    Comprehension(nested.value, name, collection.value, filter.value) on
                            startPos..afterCollection.area.end to next
                }
                else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterFilter?.area?.start ?: parser.lastPos)
            }
        }
        else -> parser.error(UNCLOSED_SQUARE_BRACKET_ERROR, afterCollection?.area?.start ?: parser.lastPos)
    }
}

fun handleObject(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> = if (token == null) error("Unclosed object") else when (token.value) {
    is ClosedCurlyBracketToken -> Object(values) on startPos..token.area.end to parser.pop()
    is IKeyToken -> parser.pop().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> {
                val exprStart = parser.pop()
                if (token.value.name == "type" && exprStart != null && exprStart.value is PureStringToken) {
                    val afterType = parser.pop()
                    return handleTypedObject(
                        parser, if (afterType?.value is SeparatorToken) parser.pop() else afterType, startPos,
                        handleTypeString(exprStart.value.name)
                    )
                }
                handleExpression(parser, exprStart)
            }
            is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
            else -> error("Missing colon or equals sign")
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
    else -> error("Invalid key name")
}

fun handleTypedObject(
    parser: Parser,
    token: PositionedToken?,
    startPos: StringPos,
    type: TypeName,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> = if (token == null) error("Unclosed object") else when (token.value) {
    is ClosedCurlyBracketToken -> Object(values, type) on startPos..token.area.end to parser.pop()
    is IKeyToken -> parser.pop().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(parser, parser.pop())
            is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
            else -> error("Missing colon or  equals sign")
        }
        when (next?.value) {
            is ClosedCurlyBracketToken ->
                Object(values + (token.value.name to expr.value), type) on
                        startPos..token.area.end to parser.pop()
            is SeparatorToken ->
                handleTypedObject(parser, parser.pop(), startPos, type, values + (token.value.name to expr.value))
            else -> handleTypedObject(parser, next, startPos, type, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}

fun handleIf(parser: Parser, token: PositionedToken?, startPos: StringPos): Pair<Positioned<If>, PositionedToken?> {
    val (cond, thenToken) = handleExpression(parser, token)
    val (thenExpr, elseToken) = handleExpression(parser, if (thenToken?.value is ThenToken) parser.pop() else thenToken)
    if (elseToken?.value !is ElseToken) error("Missing else")
    val (elseExpr, next) = handleExpression(parser, parser.pop())
    return If(cond.value, thenExpr.value, elseExpr.value) on startPos..elseExpr.area.end to next
}

fun handleTernary(
    parser: Parser,
    token: PositionedToken?,
    condExpr: IExpression,
    startPos: StringPos
): Pair<Positioned<If>, PositionedToken?> {
    val (thenExpr, colon) = handleExpression(parser, token)
    if (colon?.value !is ColonToken) error("Missing colon in ternary operation")
    val (elseExpr, next) = handleExpression(parser, parser.pop())
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
        MultiOp(Get, expr, params) on startPos..token.area.end to parser.pop()
    else -> {
        val (currentExpr, next) = handleExpression(parser, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                MultiOp(Get, expr, params + currentExpr.value) on startPos..next.area.end to parser.pop()
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
): Pair<Positioned<Where>, PositionedToken?> = if (token == null) error("Unclosed where") else when (token.value) {
    is ClosedCurlyBracketToken -> Where(expression, values) on startPos..token.area.end to parser.pop()
    is IKeyToken -> parser.pop().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(parser, parser.pop())
            is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
            else -> error("Missing equals sign")
        }
        when (next?.value) {
            is ClosedCurlyBracketToken ->
                Where(expression, values + (token.value.name to expr.value)) on
                        startPos..token.area.end to parser.pop()
            is SeparatorToken ->
                handleWhere(parser, parser.pop(), expression, startPos, values + (token.value.name to expr.value))
            else -> handleWhere(parser, next, expression, startPos, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
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
        if (parser.pop()?.value !is ArrowToken) error("Missing arrow in when expression")
        val (expr, next) = handleExpression(parser, parser.pop())
        if (elseExpr == null) handleWhen(parser, next, startPos, branches, expr.value)
        else handleWhen(parser, next, startPos, branches, elseExpr)
    }
    else -> {
        val (condition, arrow) = handleExpression(parser, token)
        if (arrow?.value !is ArrowToken) error("Missing arrow in when expression")
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
        if (parser.pop()?.value !is ArrowToken) error("Missing arrow in when expression")
        val (expr, next) = handleExpression(parser, parser.pop())
        if (elseExpr == null) handleWhenSwitch(parser, next, startPos, value, branches, expr.value)
        else handleWhenSwitch(parser, next, startPos, value, branches, elseExpr)
    }
    else -> {
        val (branch, next) = handleSwitchBranch(parser, token)
        if (elseExpr == null) handleWhenSwitch(parser, next, startPos, value, branches, elseExpr)
        else handleWhenSwitch(parser, next, startPos, value, branches + branch)
    }
}

fun handleSwitchBranch(
    parser: Parser,
    token: PositionedToken?,
    patterns: List<Pattern> = emptyList()
): Pair<SwitchBranch, PositionedToken?> {
    if (token == null) error("Unclosed when expression")
    val (pattern, separator) = when (token.value) {
        is TypeOperatorToken -> {
            val (type, next) = handleType(parser, parser.pop())
            TypePattern(type.value.clazz, token.value is IsNotToken) to next
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
            val (res, next) = handleExpression(parser, parser.pop())
            patterns + pattern to res.value to next
        }
        else -> error("Missing arrow in when expression")
    }
}