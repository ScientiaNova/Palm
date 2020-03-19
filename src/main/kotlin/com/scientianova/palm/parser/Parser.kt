package com.scientianova.palm.parser

import com.scientianova.palm.errors.PalmCompilationException
import com.scientianova.palm.errors.PalmError
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

fun parse(code: String, fileName: String = "REPL"): Object {
    val parser = Parser(tokenize(code, fileName), code, fileName)
    val first = parser.pop()
    return when (first?.value) {
        null -> Object()
        is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), StringPos(1, 1)).first.value
        else -> handleFreeObject(parser, first)
    }
}

class Parser(private val tokens: TokenList, private val code: String, private val fileName: String = "REPL") {
    fun pop() = tokens.poll()

    val lastPos = code.lines().let {
        StringPos(it.size.coerceAtLeast(1), it.lastOrNull()?.run { length + 1 } ?: 1)
    }

    fun handle(list: TokenList) = Parser(list, code, fileName)

    fun error(error: PalmError, area: StringArea): Nothing =
        throw PalmCompilationException(code, fileName, area, error)

    fun error(error: PalmError, pos: StringPos): Nothing =
        throw PalmCompilationException(code, fileName, pos..pos, error)
}

fun handleFreeObject(
    parser: Parser,
    token: PositionedToken?,
    values: Map<String, IExpression> = emptyMap()
): Object = if (token == null) Object(values) else when (token.value) {
    is IKeyToken -> parser.pop().let { assignToken ->
        val (expr, next) = when (assignToken?.value) {
            is AssignmentToken -> handleExpression(parser, parser.pop())
            is OpenCurlyBracketToken -> handleObject(parser, parser.pop(), assignToken.area.start)
            else -> error("Missing equals sign")
        }
        when (next?.value) {
            null -> Object(values + (token.value.name to expr.value))
            is SeparatorToken ->
                handleFreeObject(parser, parser.pop(), values + (token.value.name to expr.value))
            else -> handleFreeObject(parser, next, values + (token.value.name to expr.value))
        }
    }
    else -> error("Invalid key name")
}