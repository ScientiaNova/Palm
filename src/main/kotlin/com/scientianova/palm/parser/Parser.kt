package com.scientianova.palm.parser

import com.scientianova.palm.tokenizer.*

fun parse(code: String): Object {
    val stack = tokenize(code)
    val first = stack.poll()
    return when (first?.value) {
        null -> Object()
        is OpenCurlyBracketToken -> handleObject(stack, stack.poll(), 1).first.value
        else -> handleFreeObject(stack, first)
    }
}

fun handleFreeObject(
    list: TokenList,
    token: PositionedToken?,
    values: Map<String, IExpression> = emptyMap()
): Object = if (token == null) Object(values) else when (token.value) {
    is IKeyToken -> list.poll().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(list, list.poll())
            is OpenCurlyBracketToken -> handleObject(list, list.poll(), assignToken.rows.first)
            else -> error("Missing equals sign")
        }
        when (next?.value) {
            null -> Object(values + (token.value.name to expr.value))
            is SeparatorToken ->
                handleFreeObject(list, list.poll(), values + (token.value.name to expr.value))
            else -> handleFreeObject(list, next, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}