package com.scientianova.palm.parser

import com.scientianova.palm.errors.*
import com.scientianova.palm.registry.PathNode
import com.scientianova.palm.registry.RootPathNode
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.StringArea
import com.scientianova.palm.util.StringPos

class Parser(private val tokens: TokenList, private val code: String, val fileName: String = "REPL") {
    fun pop(): PositionedToken? = tokens.poll()

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

class FileAST {
    val imports = Imports()
    val objects = mutableMapOf<String, Object>()
}

fun parse(code: String, fileName: String = "REPL"): FileAST {
    val parser = Parser(tokenize(code, fileName), code, fileName)
    return handleFileStart(parser.pop(), parser, FileAST())
}

fun handleFileStart(token: PositionedToken?, parser: Parser, ast: FileAST): FileAST =
    if (token?.value is ImportToken)
        handleFileStart(handleImport(parser.pop(), parser, ast.imports), parser, ast)
    else handleTopLevelObject(token, parser, ast)

fun handleImport(
    token: PositionedToken?,
    parser: Parser,
    imports: Imports,
    lastNode: PathNode = RootPathNode
): PositionedToken? = when (val nameToken = token?.value) {
    is IdentifierToken -> {
        val next = parser.pop()
        when (next?.value) {
            is ColonToken -> handleImport(
                parser.pop(), parser, imports, lastNode[nameToken.name] ?: parser.error(INVALID_PATH_ERROR, token.area)
            )
            is AsToken -> {
                val aliasToken = parser.pop()
                val alias = aliasToken?.value as? IdentifierToken ?: parser.error(
                    INVALID_ALIAS_ERROR,
                    aliasToken?.area ?: parser.lastArea
                )
                imports += lastNode.getImports(nameToken.name, alias.name)
                parser.pop()
            }
            else -> {
                imports += lastNode.getImports(nameToken.name)
                parser.pop()
            }
        }
    }
    is TimesToken -> {
        imports += lastNode.getAllImports()
        parser.pop()
    }
    else -> parser.error(INVALID_TYPE_NAME_ERROR, token?.area ?: parser.lastArea)
}

fun handleTopLevelObject(token: PositionedToken?, parser: Parser, ast: FileAST): FileAST = when (token?.value) {
    null -> ast
    is OpenCurlyBracketToken -> {
        ast.objects[parser.fileName.substringAfterLast('/')] =
            handleObject(parser, parser.pop(), token.area.start).first.value
        handleTopLevelObject(parser.pop(), parser, ast)
    }
    else -> {
        ast.objects[parser.fileName.substringAfterLast('/')] = handleFreeObject(parser, token)
        handleTopLevelObject(parser.pop(), parser, ast)
    }
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
            else -> parser.error(MISSING_COLON_OR_EQUALS_IN_OBJECT_ERROR, assignToken?.area?.start ?: parser.lastPos)
        }
        when (next?.value) {
            null -> Object(values + (token.value.name to expr.value))
            is SeparatorToken ->
                handleFreeObject(parser, parser.pop(), values + (token.value.name to expr.value))
            else -> handleFreeObject(parser, next, values + (token.value.name to expr.value))
        }
    }
    else -> parser.error(INVALID_KEY_NAME_ERROR, token.area)
}