package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_ALIAS_ERROR
import com.scientianova.palm.errors.INVALID_IMPORT_ERROR
import com.scientianova.palm.errors.UNCLOSED_IMPORT_GROUP_ERROR
import com.scientianova.palm.tokenizer.*
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.on

interface IStatement
typealias PStatement = Positioned<IStatement>

sealed class ImportStmt : IStatement
typealias PImportStmt = Positioned<ImportStmt>

data class RegularImport(val path: PathExpr, val alias: PString) : ImportStmt()
data class OperatorImport(val path: PathExpr) : ImportStmt()
data class ModuleImport(val path: PathExpr) : ImportStmt()
data class JavaMethodImport(val path: PathExpr, val types: List<PType>, val alias: PString) : ImportStmt()
data class JavaVirtualMethodImport(val path: PathExpr, val types: List<PType>, val alias: PString) : ImportStmt()
data class JavaVirtualFieldImport(val path: PathExpr, val alias: PString) : ImportStmt()

fun handleImportStart(
    token: PToken?,
    parser: Parser,
    start: StringPos
): Pair<List<PImportStmt>, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> handleImport(token, parser, start, emptyList())
    is JavaToken -> {
        val next = parser.pop()
        when (next?.value) {
            is IdentifierToken -> handleJavaImport(next, parser, start, emptyList(), false)
            is VirtualToken -> handleJavaImport(parser.pop(), parser, start, emptyList(), true)
            is DotToken -> handleImport(parser.pop(), parser, start, listOf("java" on token.area))
            is AsToken -> {
                val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
                listOf(
                    RegularImport(
                        PathExpr(listOf(value.name on token.area)),
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                    ) on start..area.end
                ) to parser.pop()
            }
            else -> listOf(
                RegularImport(
                    PathExpr(listOf("java" on token.area)),
                    "java" on token.area
                ) on start..token.area.end
            ) to parser.pop()
        }
    }
    else -> parser.error(INVALID_IMPORT_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleImport(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString>
): Pair<List<PImportStmt>, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> {
        val next = parser.pop()
        when (next?.value) {
            is DotToken -> handleImport(parser.pop(), parser, start, path + (value.name on token.area))
            is TripleDotToken -> listOf(ModuleImport(PathExpr(path)) on start..next.area.end) to parser.pop()
            is AsToken -> {
                val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
                listOf(
                    RegularImport(
                        PathExpr(path + (value.name on token.area)),
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                    ) on start..area.end
                ) to parser.pop()
            }
            else -> listOf(
                RegularImport(PathExpr(path + (value.name on token.area)), value.name on token.area) on
                        start..token.area.end
            ) to parser.pop()
        }
    }
    is SymbolToken -> listOf(
        OperatorImport(PathExpr(path + (value.symbol on token.area))) on start..token.area.end
    ) to parser.pop()
    is OpenCurlyBracketToken -> handleImportGroup(parser.pop(), parser, start, path, emptyList())
    else -> parser.error(INVALID_IMPORT_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleImportGroup(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString>,
    parts: List<PImportStmt>
): Pair<List<PImportStmt>, PToken?> = if (token?.value is ClosedCurlyBracketToken) parts to parser.pop() else {
    val (part, symbol) = handleImport(token, parser, start, path)
    when (symbol?.value) {
        is ClosedCurlyBracketToken -> parts + part to parser.pop()
        is CommaToken -> handleImportGroup(parser.pop(), parser, start, path, parts + part)
        else -> parser.error(UNCLOSED_IMPORT_GROUP_ERROR, token?.area ?: parser.lastArea)
    }
}

tailrec fun handleJavaImport(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString>,
    virtual: Boolean
): Pair<List<PImportStmt>, PToken?> = when (val value = token?.value) {
    is IdentifierToken -> {
        val next = parser.pop()
        when (next?.value) {
            is DotToken -> handleJavaImport(parser.pop(), parser, start, path + (value.name on token.area), virtual)
            is AsToken -> {
                val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
                listOf(
                    (if (virtual)
                        JavaVirtualFieldImport(
                            PathExpr(path + (value.name on token.area)),
                            (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                        ) else RegularImport(
                        PathExpr(path + (value.name on token.area)),
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                    )) on start..area.end
                ) to parser.pop()
            }
            is OpenParenToken -> handleJavaImportParams(
                parser.pop(),
                parser,
                start,
                PathExpr(path + (value.name on token.area)),
                virtual,
                emptyList()
            )
            else -> listOf(
                (if (virtual)
                    JavaVirtualFieldImport(PathExpr(path + (value.name on token.area)), value.name on token.area)
                else RegularImport(PathExpr(path + (value.name on token.area)), value.name on token.area)
                        ) on start..token.area.end
            ) to parser.pop()
        }
    }
    is OpenCurlyBracketToken -> handleJavaImportGroup(parser.pop(), parser, start, path, emptyList(), virtual)
    else -> parser.error(INVALID_IMPORT_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleJavaImportParams(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: PathExpr,
    virtual: Boolean,
    params: List<PType>
): Pair<List<PImportStmt>, PToken?> = if (token?.value is ClosedParenToken) {
    val next = parser.pop()
    if (next?.value is AsToken) {
        val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
        listOf(
            (if (virtual) JavaVirtualMethodImport(
                path, params, (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
            ) else JavaMethodImport(
                path, params, (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
            )) on start..area.end
        ) to parser.pop()
    } else listOf(
        (if (virtual) JavaVirtualMethodImport(path, params, path.parts.last())
        else JavaMethodImport(path, params, path.parts.last())) on start..token.area.end
    ) to next
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is ClosedParenToken -> {
            val next = parser.pop()
            if (next?.value is AsToken) {
                val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
                listOf(
                    (if (virtual) JavaVirtualMethodImport(
                        path, params + type,
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                    ) else JavaMethodImport(
                        path, params + type,
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name on area
                    )) on start..area.end
                ) to parser.pop()
            } else listOf(
                (if (virtual) JavaVirtualMethodImport(path, params + type, path.parts.last())
                else JavaMethodImport(path, params + type, path.parts.last())) on start..symbol.area.end
            ) to next
        }
        is CommaToken -> handleJavaImportParams(parser.pop(), parser, start, path, virtual, params + type)
        else -> parser.error(INVALID_IMPORT_ERROR, token?.area ?: parser.lastArea)
    }
}

tailrec fun handleJavaImportGroup(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString>,
    parts: List<PImportStmt>,
    virtual: Boolean
): Pair<List<PImportStmt>, PToken?> = if (token?.value is ClosedCurlyBracketToken) parts to parser.pop() else {
    val (part, symbol) = handleJavaImport(token, parser, start, path, virtual)
    when (symbol?.value) {
        is ClosedCurlyBracketToken -> parts + part to parser.pop()
        is CommaToken -> handleJavaImportGroup(parser.pop(), parser, start, path, parts + part, virtual)
        else -> parser.error(UNCLOSED_IMPORT_GROUP_ERROR, token?.area ?: parser.lastArea)
    }
}