package com.scientianovateam.palm.parser

import com.scientianovateam.palm.util.flip
import com.scientianovateam.palm.util.safePop
import com.scientianovateam.palm.tokenizer.*

fun parse(code: String): Object {
    val stack = tokenize(code).flip()
    val first = stack.safePop()
    return when (first?.value) {
        null -> Object()
        is OpenCurlyBracketToken -> handleObject(stack, stack.safePop(), 1).first.value
        else -> handleFreeObject(stack, first)
    }
}

fun handleFreeObject(
    stack: TokenStack,
    token: PositionedToken?,
    values: Map<String, IExpression> = emptyMap()
): Object = if (token == null) Object(values) else when (token.value) {
    is IKeyToken -> stack.safePop().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(stack, stack.safePop())
            is OpenCurlyBracketToken -> handleObject(stack, stack.safePop(), assignToken.rows.first)
            else -> error("Missing equals sign")
        }
        when (next?.value) {
            null -> Object(values + (token.value.name to expr.value))
            is CommaToken ->
                handleFreeObject(stack, stack.safePop(), values + (token.value.name to expr.value))
            else -> handleFreeObject(stack, next, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}