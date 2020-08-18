package com.scientianova.palm.parser

import com.scientianova.palm.errors.missingExpressionSeparatorError

fun handleImports(startState: ParseState) = reuseWhileSuccess(startState, emptyList<Import>()) { list, state ->
    when {
        state.char == ';' -> list succTo state.nextActual
        state.startWithIdent("import") -> handleImportStart((state + 6).actual).flatMap { newImports, afterImport ->
            val sepState = afterImport.actual
            when (sepState.char) {
                '\n', ';' -> list + newImports succTo sepState.next
                else -> missingExpressionSeparatorError failAt sepState
            }
        }
        else -> list succTo state
    }
}