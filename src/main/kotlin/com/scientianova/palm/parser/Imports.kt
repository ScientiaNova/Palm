package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidImportError
import com.scientianova.palm.errors.unclosedImportGroupError
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class Import
typealias PImport = Positioned<Import>

data class RegularImport(val path: List<PString>, val alias: PString) : Import()
data class OperatorImport(val path: List<PString>) : Import()
data class PackageImport(val path: List<PString>) : Import()

fun handleImportStart(
    state: ParseState,
    start: StringPos
): ParseResult<List<PImport>> = if (state.char?.isLetter() == true) {
    val (ident, afterIdent) = handleIdent(state)
    handleImport(afterIdent, start, listOf(ident))
} else invalidImportError errAt state.pos

tailrec fun handleImport(
    state: ParseState,
    start: StringPos,
    path: List<PString>
): ParseResult<List<PImport>> = if (state.char == '.') {
    val char = state.nextChar
    when {
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdent(state.next)
            if (ident.value == "_") listOf(PackageImport(path) at start..state.nextPos) succTo afterIdent
            else handleImport(afterIdent, start, path + ident)
        }
        char?.isSymbolPart() == true -> {
            val (symbol, next) = handleSymbol(state.next)
            listOf(OperatorImport(path + symbol) at start..symbol.area.last) succTo next
        }
        char == '{' -> handleImportGroup(state + 2, start, path, emptyList())
        else -> invalidImportError errAt state.pos
    }
} else {
    val maybeAsState = state.actual
    val (maybeAs, afterAs) = handleIdent(maybeAsState)
    if (maybeAs.value == "as") {
        val (alias, next) = handleIdent(afterAs.actual)
        listOf(RegularImport(path, alias) at start..alias.area.last) succTo next
    } else listOf(RegularImport(path, path.last()) at start..path.last().area.last) succTo state
}

tailrec fun handleImportGroup(
    state: ParseState,
    start: StringPos,
    path: List<PString>,
    parts: List<PImport>
): ParseResult<List<PImport>> = if (state.char == '}') parts succTo state.next
else handleImport(state, start, path).flatMap { part, afterState ->
    val symbolState = afterState.actual
    when (symbolState.char) {
        '}' -> parts + part succTo symbolState.next
        ',' -> return handleImportGroup(symbolState.nextActual, start, path, parts + part)
        else -> unclosedImportGroupError errAt state.pos
    }
}