package com.scientianova.palm.parser

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


fun handleExpression(list: TokenList, token: PositionedToken?): Pair<PositionedExpression, PositionedToken?> {
    val (first, op) = handleExpressionPart(list, token)
    val expr: PositionedExpression
    val next: PositionedToken?
    if (op != null && op.value is OperatorToken && (op.value !is IUnaryOperatorToken || op.area.end.row == first.area.start.row)) {
        val res = handleBinOps(list, op.value, first.area.start, Stack<IOperationPart>().pushing(first.value))
        expr = res.first
        next = res.second
    } else {
        expr = first
        next = op
    }
    return if (next?.value is WhereToken) {
        if (list.poll()?.value is OpenCurlyBracketToken)
            handleWhere(list, list.poll(), expr.value, expr.area.start)
        else error("Missing open curly bracket after where")
    } else expr to next
}

fun handleBinOps(
    list: TokenList,
    op: OperatorToken,
    startPos: StringPos,
    operandStack: Stack<IOperationPart>,
    operatorStack: Stack<OperatorToken> = Stack()
): Pair<PositionedExpression, PositionedToken?> =
    if (operatorStack.isEmpty() || op.precedence > operatorStack.peek().precedence) {
        if (op == TernaryOperatorToken)
            handleTernary(list, list.poll(), operandStack.pop() as IExpression, startPos)
        else {
            val (operand, next) =
                if (op is TypeOperatorToken) handleType(list, list.poll())
                else handleExpressionPart(list, list.poll())
            operatorStack.push(op)
            operandStack.push(operand.value)
            if (next != null && next.value is OperatorToken && (next.value !is IUnaryOperatorToken || next.area.end.row == operand.area.start.row)) {
                if (op is ComparisonOperatorToken && next.value is ComparisonOperatorToken) {
                    operatorStack.push(AndToken)
                    operandStack.push(operand.value)
                }
                handleBinOps(list, next.value, startPos, operandStack, operatorStack)
            } else emptyStacks(next, startPos..operand.area.end, operandStack, operatorStack)
        }
    } else {
        val second = operandStack.pop()
        handleBinOps(
            list, op, startPos, operandStack.pushing(operatorStack.pop().handleExpression(operandStack.pop(), second)),
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

fun handleExpressionPart(list: TokenList, token: PositionedToken?): Pair<PositionedExpression, PositionedToken?> {
    val (expr, afterExpr) = if (token == null) error("Missing expression part") else when (token.value) {
        is NumberToken -> Num(token.value.number) on token to list.poll()
        is CharToken -> Chr(token.value.char) on token to list.poll()
        is BoolToken -> Bool(token.value.bool) on token to list.poll()
        is NullToken -> Null on token to list.poll()
        is IdentifierToken ->
            handleIdentifierSequence(list, list.poll(), token.area.start, listOf(token.value.name))
        is OpenCurlyBracketToken ->
            handleObject(list, list.poll(), token.area.start)
        is OpenParenToken -> {
            val (expr, closedParen) = handleExpression(list, list.poll())
            if (closedParen?.value is ClosedParenToken) expr to list.poll() else error("Unclosed parenthesis")
        }
        is OpenSquareBracketToken -> {
            val first = list.poll()
            if (first?.value is ClosedSquareBracketToken)
                Lis() on token.area.start..first.area.end to list.poll()
            else {
                val (expr, next) = handleExpression(list, first)
                when (next?.value) {
                    is CommaToken ->
                        handleSecondInList(list, list.poll(), token.area.start, expr.value)
                    is SemicolonToken ->
                        handleList(list, list.poll(), token.area.start, emptyList(), listOf(Lis(expr.value)))
                    is ColonToken -> {
                        val (value, newNext) = handleExpression(list, list.poll())
                        if (newNext?.value is CommaToken)
                            handleDict(list, list.poll(), token.area.start, mapOf(expr.value to value.value))
                        else handleDict(list, newNext, token.area.start, mapOf(expr.value to value.value))
                    }
                    is DoubleDotToken -> {
                        val (last, closedBracket) = handleExpression(list, list.poll())
                        if (closedBracket?.value is ClosedSquareBracketToken)
                            MultiOp(RangeTo, expr.value, listOf(last.value)) on
                                    token.area.start..closedBracket.area.end to list.poll()
                        else error("Unclosed square bracket")
                    }
                    is ForToken -> handleComprehension(list, list.poll(), expr.value, token.area.start)
                    else -> handleSecondInList(list, next, token.area.start, expr.value)
                }
            }
        }
        is PureStringToken -> Str(listOf(StrStringPart(token.value.name))) on token to list.poll()
        is StringTemplateToken -> Str(token.value.parts.map {
            when (it) {
                is StringPart -> StrStringPart(it.string)
                is TokensPart -> {
                    val (expr, nullToken) = handleExpression(it.tokens, it.tokens.poll())
                    if (nullToken != null) error("Invalid interpolated expression")
                    StrExpressionPart(expr.value)
                }
            }
        }) on token to list.poll()
        is IUnaryOperatorToken -> {
            val (expr, next) = handleExpressionPart(list, list.poll())
            UnaryOp(token.value.unaryOp, expr.value) on token.area.start..expr.area.end to next
        }
        is IfToken -> handleIf(list, list.poll(), token.area.start)
        is WhenToken -> {
            val afterWhen = list.poll()
            if (afterWhen?.value is OpenCurlyBracketToken) handleWhen(list, list.poll(), token.area.start)
            else {
                val (expr, bracket) = handleExpression(list, list.poll())
                if (bracket?.value is OpenCurlyBracketToken)
                    handleWhenSwitch(list, list.poll(), token.area.start, expr.value)
                else error("Missing curly bracket after when")
            }
        }
        else -> error("Invalid expression part")
    }
    return handlePostfixOperations(list, afterExpr, expr)
}

fun handlePostfixOperations(
    list: TokenList,
    token: PositionedToken?,
    expr: PositionedExpression
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is DotToken -> {
        val name = list.poll()
        if (name == null || name.value !is IdentifierToken) error("Invalid field name")
        handlePostfixOperations(
            list, list.poll(), ValAccess(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is SafeAccessToken -> {
        val name = list.poll()
        if (name == null || name.value !is IdentifierToken) error("Invalid field name")
        handlePostfixOperations(
            list, list.poll(), SafeValAccess(expr.value, name.value.name) on expr.area.start..name.area.end
        )
    }
    is OpenSquareBracketToken -> {
        val (getter, next) = handleGetter(list, list.poll(), expr.value, expr.area.start)
        handlePostfixOperations(list, next, getter)
    }
    else -> expr to token
}

fun handleIdentifierSequence(
    queue: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    list: List<String>
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is DotToken -> {
        val top = queue.poll()?.value as? IdentifierToken ?: error("Invalid field name")
        handleIdentifierSequence(queue, queue.poll(), startPos, list + top.name)
    }
    is OpenCurlyBracketToken ->
        handleTypedObject(
            queue, queue.poll(), startPos, TypeName(list.dropLast(1).joinToString(".") { it }, list.last())
        )
    else -> list.drop(1).fold(Constant(list.first()) as IExpression) { acc, s -> ValAccess(acc, s) } on
            startPos to token
}

fun handleSecondInList(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    first: IExpression
): Pair<PositionedExpression, PositionedToken?> = when {
    token == null -> error("Unclosed square bracket")
    token.value is ClosedSquareBracketToken -> Lis(first) on startPos..token.area.end to list.poll()
    else -> {
        val (expr, next) = handleExpression(list, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                Lis(listOf(first, expr.value)) on startPos..token.area.end to list.poll()
            is CommaToken ->
                handleList(list, list.poll(), startPos, listOf(first, expr.value))
            is SemicolonToken ->
                handleList(list, list.poll(), startPos, emptyList(), listOf(Lis(listOf(first, expr.value))))
            is DoubleDotToken -> {
                val (last, closedBracket) = handleExpression(list, list.poll())
                if (closedBracket?.value is ClosedSquareBracketToken)
                    MultiOp(RangeTo, first, listOf(expr.value, last.value)) on startPos..closedBracket.area.end to
                            list.poll()
                else error("Unclosed square bracket")
            }
            else -> handleList(list, next, startPos, listOf(first, expr.value))
        }
    }
}

fun handleList(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    values: List<IExpression>,
    lists: List<Lis> = emptyList()
): Pair<Positioned<Lis>, PositionedToken?> = when {
    token == null -> error("Unclosed square bracket")
    token.value is ClosedSquareBracketToken ->
        (if (lists.isEmpty()) Lis(values) else Lis(lists + Lis(values))) on startPos..token.area.end to list.poll()
    else -> {
        val (expr, next) = handleExpression(list, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                (if (lists.isEmpty()) Lis(values + expr.value) else Lis(lists + Lis(values + expr.value))) on
                        startPos..token.area.end to list.poll()
            is CommaToken ->
                handleList(list, list.poll(), startPos, values + expr.value, lists)
            is SemicolonToken ->
                handleList(list, list.poll(), startPos, emptyList(), lists + Lis(values + expr.value))
            else -> handleList(list, next, startPos, values + expr.value, lists)
        }
    }
}

fun handleDict(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    values: Map<IExpression, IExpression>
): Pair<Positioned<Dict>, PositionedToken?> = when {
    token == null -> error("Unclosed square bracket")
    token.value is ClosedSquareBracketToken -> Dict(values) on startPos..token.area.end to list.poll()
    else -> {
        val (key, colon) = handleExpression(list, token)
        if (colon?.value !is ColonToken) error("Missing colon in dict")
        val (value, next) = handleExpression(list, list.poll())
        when (next?.value) {
            is ClosedSquareBracketToken ->
                Dict(values + (key.value to value.value)) on startPos..token.area.end to list.poll()
            is CommaToken ->
                handleDict(list, list.poll(), startPos, values + (key.value to value.value))
            else -> handleDict(list, colon, startPos, values + (key.value to value.value))
        }
    }
}

fun handleComprehension(
    list: TokenList,
    token: PositionedToken?,
    expr: IExpression,
    startPos: StringPos
): Pair<PositionedExpression, PositionedToken?> {
    if (token == null || token.value !is IdentifierToken)
        error("Invalid variable name in list comprehension")
    val name = token.value.name
    if (list.poll()?.value !is InToken) error("Missing `in` in list comprehension")
    val (collection, afterCollection) = handleExpression(list, list.poll())
    return when (afterCollection?.value) {
        is ClosedSquareBracketToken ->
            Comprehension(expr, name, collection.value) on startPos..afterCollection.area.end to list.poll()
        is ForToken -> {
            val (nested, next) = handleComprehension(list, list.poll(), expr, startPos)
            Comprehension(nested.value, name, collection.value) on startPos..afterCollection.area.end to next
        }
        is IfToken -> {
            val (filter, afterFilter) = handleExpression(list, list.poll())
            when (afterFilter?.value) {
                is ClosedSquareBracketToken -> Comprehension(expr, name, collection.value, filter.value) on
                        startPos..afterCollection.area.end to list.poll()
                is ForToken -> {
                    val (nested, next) = handleComprehension(list, list.poll(), expr, startPos)
                    Comprehension(nested.value, name, collection.value, filter.value) on
                            startPos..afterCollection.area.end to next
                }
                else -> error("Unclosed square bracket")
            }
        }
        else -> error("Unclosed square bracket")
    }
}

fun handleObject(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> = if (token == null) error("Unclosed object") else when (token.value) {
    is ClosedCurlyBracketToken -> Object(values) on startPos..token.area.end to list.poll()
    is IKeyToken -> list.poll().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> {
                val exprStart = list.poll()
                if (token.value.name == "type" && exprStart != null && exprStart.value is PureStringToken) {
                    val afterType = list.poll()
                    return handleTypedObject(
                        list, if (afterType?.value is SeparatorToken) list.poll() else afterType, startPos,
                        handleTypeString(exprStart.value.name)
                    )
                }
                handleExpression(list, exprStart)
            }
            is OpenCurlyBracketToken -> handleObject(list, list.poll(), assignToken.area.start)
            else -> error("Missing colon or equals sign")
        }
        when (next?.value) {
            is ClosedCurlyBracketToken ->
                Object(values + (token.value.name to expr.value)) on
                        startPos..token.area.end to list.poll()
            is SeparatorToken ->
                handleObject(list, list.poll(), startPos, values + (token.value.name to expr.value))
            else -> handleObject(list, next, startPos, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}

fun handleTypedObject(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    type: TypeName,
    values: Map<String, IExpression> = emptyMap()
): Pair<Positioned<Object>, PositionedToken?> = if (token == null) error("Unclosed object") else when (token.value) {
    is ClosedCurlyBracketToken -> Object(values, type) on startPos..token.area.end to list.poll()
    is IKeyToken -> list.poll().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(list, list.poll())
            is OpenCurlyBracketToken -> handleObject(list, list.poll(), assignToken.area.start)
            else -> error("Missing colon or  equals sign")
        }
        when (next?.value) {
            is ClosedCurlyBracketToken ->
                Object(values + (token.value.name to expr.value), type) on
                        startPos..token.area.end to list.poll()
            is SeparatorToken ->
                handleTypedObject(list, list.poll(), startPos, type, values + (token.value.name to expr.value))
            else -> handleTypedObject(list, next, startPos, type, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}

fun handleIf(list: TokenList, token: PositionedToken?, startPos: StringPos): Pair<Positioned<If>, PositionedToken?> {
    val (cond, thenToken) = handleExpression(list, token)
    if (thenToken?.value !is ThenToken) error("Missing then")
    val (thenExpr, elseToken) = handleExpression(list, list.poll())
    if (elseToken?.value !is ElseToken) error("Missing else")
    val (elseExpr, next) = handleExpression(list, list.poll())
    return If(cond.value, thenExpr.value, elseExpr.value) on startPos..elseExpr.area.end to next
}

fun handleTernary(
    list: TokenList,
    token: PositionedToken?,
    condExpr: IExpression,
    startPos: StringPos
): Pair<Positioned<If>, PositionedToken?> {
    val (thenExpr, colon) = handleExpression(list, token)
    if (colon?.value !is ColonToken) error("Missing colon in ternary operation")
    val (elseExpr, next) = handleExpression(list, list.poll())
    return If(condExpr, thenExpr.value, elseExpr.value) on startPos..elseExpr.area.end to next
}

fun handleGetter(
    list: TokenList,
    token: PositionedToken?,
    expr: IExpression,
    startPos: StringPos,
    params: List<IExpression> = emptyList()
): Pair<PositionedExpression, PositionedToken?> = when {
    token == null -> error("Unclosed square bracket")
    token.value is ClosedSquareBracketToken ->
        MultiOp(Get, expr, params) on startPos..token.area.end to list.poll()
    else -> {
        val (currentExpr, next) = handleExpression(list, token)
        when (next?.value) {
            is ClosedSquareBracketToken ->
                MultiOp(Get, expr, params + currentExpr.value) on startPos..next.area.end to list.poll()
            is CommaToken ->
                handleGetter(list, list.poll(), expr, startPos, params + currentExpr.value)
            else -> handleGetter(list, next, expr, startPos, params + currentExpr.value)
        }
    }
}

fun handleWhere(
    list: TokenList,
    token: PositionedToken?,
    expression: IExpression,
    startPos: StringPos,
    values: List<Pair<String, IExpression>> = emptyList()
): Pair<Positioned<Where>, PositionedToken?> = if (token == null) error("Unclosed where") else when (token.value) {
    is ClosedCurlyBracketToken -> Where(expression, values) on startPos..token.area.end to list.poll()
    is IKeyToken -> list.poll().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(list, list.poll())
            is OpenCurlyBracketToken -> handleObject(list, list.poll(), assignToken.area.start)
            else -> error("Missing equals sign")
        }
        when (next?.value) {
            is ClosedCurlyBracketToken ->
                Where(expression, values + (token.value.name to expr.value)) on
                        startPos..token.area.end to list.poll()
            is SeparatorToken ->
                handleWhere(list, list.poll(), expression, startPos, values + (token.value.name to expr.value))
            else -> handleWhere(list, next, expression, startPos, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}

fun handleWhen(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    branches: List<Pair<IExpression, IExpression>> = emptyList(),
    elseExpr: IExpression? = null
): Pair<Positioned<When>, PositionedToken?> = when (token?.value) {
    is ClosedCurlyBracketToken -> When(branches, elseExpr) on startPos..token.area.end to list.poll()
    is ElseToken -> {
        if (list.poll()?.value !is ArrowToken) error("Missing arrow in when expression")
        val (expr, next) = handleExpression(list, list.poll())
        if (elseExpr == null) handleWhen(list, next, startPos, branches, expr.value)
        else handleWhen(list, next, startPos, branches, elseExpr)
    }
    else -> {
        val (condition, arrow) = handleExpression(list, token)
        if (arrow?.value !is ArrowToken) error("Missing arrow in when expression")
        val (expr, next) = handleExpression(list, list.poll())
        if (elseExpr == null) handleWhen(list, next, startPos, branches + (condition.value to expr.value))
        else handleWhen(list, next, startPos, branches, elseExpr)
    }
}

fun handleWhenSwitch(
    list: TokenList,
    token: PositionedToken?,
    startPos: StringPos,
    value: IExpression,
    branches: List<SwitchBranch> = emptyList(),
    elseExpr: IExpression? = null
): Pair<PositionedExpression, PositionedToken?> = when (token?.value) {
    is ClosedCurlyBracketToken -> WhenSwitch(value, branches, elseExpr) on startPos..token.area.end to list.poll()
    is ElseToken -> {
        if (list.poll()?.value !is ArrowToken) error("Missing arrow in when expression")
        val (expr, next) = handleExpression(list, list.poll())
        if (elseExpr == null) handleWhenSwitch(list, next, startPos, value, branches, expr.value)
        else handleWhenSwitch(list, next, startPos, value, branches, elseExpr)
    }
    else -> {
        val (branch, next) = handleSwitchBranch(list, token)
        if (elseExpr == null) handleWhenSwitch(list, next, startPos, value, branches, elseExpr)
        else handleWhenSwitch(list, next, startPos, value, branches + branch)
    }
}

fun handleSwitchBranch(
    list: TokenList,
    token: PositionedToken?,
    patterns: List<Pattern> = emptyList()
): Pair<SwitchBranch, PositionedToken?> {
    if (token == null) error("Unclosed when expression")
    val (pattern, separator) = when (token.value) {
        is TypeOperatorToken -> {
            val (type, next) = handleType(list, list.poll())
            TypePattern(type.value.clazz, token.value is IsNotToken) to next
        }
        is ContainingOperatorToken -> {
            val (expr, next) = handleExpression(list, list.poll())
            ContainingPattern(expr.value, token.value is NotInToken) to next
        }
        is ComparisonOperatorToken -> {
            val (expr, next) = handleExpression(list, list.poll())
            ComparisonPattern(token.value, expr.value) to next
        }
        else -> {
            val (expr, next) = handleExpression(list, token)
            ExpressionPattern(expr.value) to next
        }
    }
    return when (separator?.value) {
        is CommaToken -> handleSwitchBranch(list, list.poll(), patterns + pattern)
        is ArrowToken -> {
            val (res, next) = handleExpression(list, list.poll())
            patterns + pattern to res.value to next
        }
        else -> error("Missing arrow in when expression")
    }
}