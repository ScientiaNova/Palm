package com.scientianova.palm.parser

fun handleImports(startState: ParseState) = reuseWhileSuccess(startState, emptyList<Import>()) { list, state ->
    when {
        state.char == '\n' -> list succTo state.nextActual
        state.startWithIdent("import") -> handleImportStart((state + 6).actual).map { list + it }
        else -> list succTo state
    }
}