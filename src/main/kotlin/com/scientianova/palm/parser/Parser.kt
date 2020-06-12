package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

class Parser(private val tokens: TokenList, private val code: String, val fileName: String = "REPL") {
    fun pop(): PToken? = tokens.poll()

    val lastPos = code.lines().let {
        StringPos(it.size.coerceAtLeast(1), it.lastOrNull()?.run { length + 1 } ?: 1)
    }

    val lastArea get() = lastPos..lastPos

    fun handle(list: TokenList) = Parser(list, code, fileName)

    fun error(error: PalmError, area: StringArea): Nothing =
        throw PalmCompilationException(code, fileName, area, error)

    fun error(error: PalmError, pos: StringPos): Nothing =
        throw PalmCompilationException(code, fileName, pos..pos, error)
}

fun parse(code: String, fileName: String = "REPL"): ParsedModule {
    val parser = Parser(tokenize(code, fileName), code, fileName)
    return handleTopLevel(parser.pop(), parser, ParsedModule())
}

tailrec fun handleTopLevel(token: PToken?, parser: Parser, module: ParsedModule): ParsedModule = when (token?.value) {
    null -> module
    is ImportToken -> {
        val (imports, next) = handleImportStart(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleTopLevel(actualNext, parser, module.copy(statements = module.statements + imports))
    }
    is LetToken -> {
        val (declaration, next) = handleDeclaration(parser.pop(), parser, token.area.start)
        val actualNext = when (next?.value) {
            is CommaToken, is SemicolonToken -> parser.pop()
            else -> next
        }
        handleTopLevel(actualNext, parser, module.with(declaration))
    }
    else -> {
        val (expr, next) = handleExpression(token, parser, inScope = true)
        when (next?.value) {
            is EqualsToken -> if (expr.value is CallExpr) {
                if (expr.value.expr.value !is PathExpr) parser.error(INVALID_FUNCTION_NAME, expr.value.expr.area)
                if (expr.value.expr.value.parts.size != 1) parser.error(INVALID_FUNCTION_NAME, expr.value.expr.area)
                val name = expr.value.expr.value.parts.first()
                val (funExpr, next1) = handleExpression(parser.pop(), parser)
                val actualNext = when (next1?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next1
                }
                handleTopLevel(
                    actualNext, parser, module.with(
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
                handleTopLevel(
                    actualNext, parser, module.with(ConstAssignment(pattern, valueExpr, false) on expr.area.start..valueExpr.area.end)
                )
            }
            else -> {
                val actualNext = when (next?.value) {
                    is CommaToken, is SemicolonToken -> parser.pop()
                    else -> next
                }
                handleTopLevel(actualNext, parser, module.with(expr))
            }
        }
    }
}