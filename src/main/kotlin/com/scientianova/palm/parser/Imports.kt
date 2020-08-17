package com.scientianova.palm.parser

import com.scientianova.palm.errors.invalidImportError
import com.scientianova.palm.errors.unclosedImportGroupError
import com.scientianova.palm.util.PString

sealed class Import

data class RegularImport(val path: List<PString>, val alias: PString) : Import()
data class OperatorImport(val path: List<PString>) : Import()
data class PackageImport(val path: List<PString>) : Import()

fun handleImportStart(
    state: ParseState
): ParseResult<List<Import>> = if (state.char?.isIdentifierStart() == true) {
    val (ident, afterIdent) = handleIdent(state)
    handleImport(afterIdent, listOf(ident))
} else invalidImportError errAt state.pos

private tailrec fun handleImport(
    state: ParseState,
    path: List<PString>
): ParseResult<List<Import>> = if (state.char == '.') when (state.nextChar) {
    in identStartChars -> {
        val (ident, afterIdent) = handleIdent(state.next)
        if (ident.value == "_") listOf(PackageImport(path)) succTo afterIdent
        else handleImport(afterIdent, path + ident)
    }
    in symbolChars -> {
        val (symbol, next) = handleSymbol(state.next)
        listOf(OperatorImport(path + symbol)) succTo next
    }
    '{' -> handleImportGroup(state + 2, path, emptyList())
    else -> invalidImportError errAt state.pos
} else {
    val maybeAsState = state.actual
    if (maybeAsState.startWithIdent("as")) {
        val (alias, next) = handleIdent((maybeAsState + 2).actual)
        listOf(RegularImport(path, alias)) succTo next
    } else listOf(RegularImport(path, path.last())) succTo state
}

private fun handleImportGroup(
    state: ParseState,
    path: List<PString>,
    parts: List<Import>
): ParseResult<List<Import>> = if (state.char == '}') parts succTo state.next
else handleImport(state, path).flatMap { part, afterState ->
    val symbolState = afterState.actual
    when (symbolState.char) {
        '}' -> parts + part succTo symbolState.next
        ',' -> handleImportGroup(symbolState.nextActual, path, parts + part)
        else -> unclosedImportGroupError errAt state.pos
    }
}