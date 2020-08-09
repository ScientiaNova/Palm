package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidImportError
import com.scientianova.palm.errors.unclosedImportGroupError
import com.scientianova.palm.errors.throwAt
import com.scientianova.palm.util.PString
import com.scientianova.palm.util.Positioned
import com.scientianova.palm.util.StringPos
import com.scientianova.palm.util.at

sealed class Import
typealias PImport = Positioned<Import>

data class RegularImport(val path: List<PString>, val alias: PString) : Import()
data class OperatorImport(val path: List<PString>) : Import()
data class ModuleImport(val path: List<PString>) : Import()

fun handleImportStart(
    state: ParseState,
    start: StringPos
): Pair<List<PImport>, ParseState> = if (state.char?.isLetter() == true) {
    val (ident, afterIdent) = handleIdentifier(state)
    handleImport(afterIdent, start, listOf(ident))
} else invalidImportError throwAt state.pos

tailrec fun handleImport(
    state: ParseState,
    start: StringPos,
    path: List<PString>
): Pair<List<PImport>, ParseState> = if (state.char == '.') {
    val char = state.nextChar
    when {
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdentifier(state.next)
            if (ident.value == "_") listOf(ModuleImport(path) at start..state.nextPos) to afterIdent
            else handleImport(afterIdent, start, path + ident)
        }
        char?.isSymbolPart() == true -> {
            val (symbol, next) = handleSymbol(state.next)
            listOf(OperatorImport(path + symbol) at start..symbol.area.last) to next
        }
        char == '{' -> handleImportGroup(state + 2, start, path, emptyList())
        else -> invalidImportError throwAt state.pos
    }
} else {
    val maybeAsState = state.actual
    val (maybeAs, afterAs) = handleIdentifier(maybeAsState)
    if (maybeAs.value == "as") {
        val (alias, next) = handleIdentifier(afterAs.actual)
        listOf(RegularImport(path, alias) at start..alias.area.last) to next
    } else listOf(RegularImport(path, path.last()) at start..path.last().area.last) to state
}

tailrec fun handleImportGroup(
    state: ParseState,
    start: StringPos,
    path: List<PString>,
    parts: List<PImport>
): Pair<List<PImport>, ParseState> = if (state.char == '}') parts to state.next else {
    val (part, afterState) = handleImport(state, start, path)
    val symboState = afterState.actual
    when (symboState.char) {
        '}' -> parts + part to symboState.next
        ',' -> handleImportGroup(symboState.nextActual, start, path, parts + part)
        else -> unclosedImportGroupError throwAt state.pos
    }
}