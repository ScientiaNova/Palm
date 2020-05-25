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

tailrec fun handleImport(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString> = emptyList()
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
    is OpenCurlyBracketToken -> handleImportGroup(parser.pop(), parser, start, path)
    else -> parser.error(INVALID_IMPORT_ERROR, token?.area ?: parser.lastArea)
}

tailrec fun handleImportGroup(
    token: PToken?,
    parser: Parser,
    start: StringPos,
    path: List<PString>,
    parts: List<PImportStmt> = emptyList()
): Pair<List<PImportStmt>, PToken?> = if (token?.value is ClosedCurlyBracketToken) parts to parser.pop() else {
    val (part, symbol) = handleImport(token, parser, start, path)
    when (symbol?.value) {
        is ClosedCurlyBracketToken -> parts + part to parser.pop()
        is CommaToken -> handleImportGroup(parser.pop(), parser, start, path, parts + part)
        else -> parser.error(UNCLOSED_IMPORT_GROUP_ERROR, token?.area ?: parser.lastArea)
    }
}