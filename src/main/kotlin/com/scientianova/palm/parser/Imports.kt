package com.scientianova.palm.parser

import com.scientianova.palm.errors.INVALID_ALIAS_ERROR
import com.scientianova.palm.errors.INVALID_IMPORT_ERROR
import com.scientianova.palm.errors.UNCLOSED_IMPORT_GROUP_ERROR
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
data class JavaMethodImport(val path: List<PString>, val types: List<PType>, val alias: PString) : Import()
data class JavaVirtualMethodImport(val path: List<PString>, val types: List<PType>, val alias: PString) : Import()
data class JavaVirtualFieldImport(val path: List<PString>, val alias: PString) : Import()

fun handleImportStart(
    state: ParseState,
    start: StringPos
): Pair<List<PImport>, ParseState> = if (state.char?.isLetter() == true) {
    val (ident, afterIdent) = handleIdentifier(state)
    if (ident.value == "java" && afterIdent.char?.isLineSpace() == true) {
        val (actualFirst, afterFirst) = handleIdentifier(afterIdent.actualOrBreak)
        if (actualFirst.value.isEmpty())
            INVALID_IMPORT_ERROR throwAt afterFirst.pos
        if (actualFirst.value == "virtual" && afterFirst.char?.isLineSpace() == true) {
            val (actualFirst1, afterFirst1) = handleIdentifier(afterFirst.actualOrBreak)
            if (actualFirst1.value.isEmpty())
                INVALID_IMPORT_ERROR throwAt afterFirst1.pos
            handleJavaImport(afterFirst, start, listOf(actualFirst1), true)
        } else handleJavaImport(afterFirst, start, listOf(actualFirst), false)
    } else handleImport(afterIdent, start, listOf(ident))
} else INVALID_IMPORT_ERROR throwAt state.pos

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
        else -> INVALID_IMPORT_ERROR throwAt state.pos
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
        else -> UNCLOSED_IMPORT_GROUP_ERROR throwAt state.pos
    }
}

tailrec fun handleJavaImport(
    state: ParseState,
    start: StringPos,
    path: List<PString>,
    virtual: Boolean
): Pair<List<PImport>, ParseState> = if (state.char == '.') {
    val char = state.nextChar
    when {
        char?.isLetter() == true -> {
            val (ident, afterIdent) = handleIdentifier(state.next)
            handleJavaImport(afterIdent, start, path + ident, virtual)
        }
        char == '{' -> handleJavaImportGroup(state + 2, start, path, emptyList())
        else -> INVALID_IMPORT_ERROR throwAt state.pos
    }
} else {
    val maybeAsState = state.actual
    if (maybeAsState.char == '(') {
        handleJavaImportParams(maybeAsState.nextActual, maybeAsState.pos, path, virtual, emptyList())
    } else {
        val (maybeAs, afterAs) = handleIdentifier(maybeAsState)
        if (maybeAs.value == "as") {
            val (alias, next) = handleIdentifier(afterAs.actual)
            listOf(
                (if (virtual) JavaVirtualFieldImport(path, alias) else RegularImport(path, alias))
                        at start..alias.area.last
            ) to next
        } else listOf(
            (if (virtual) JavaVirtualFieldImport(path, path.last()) else RegularImport(path, path.last()))
                    at start..path.last().area.last
        ) to state
    }
}

tailrec fun handleJavaImportParams(
    state: ParseState,
    start: StringPos,
    path: List<PString>,
    virtual: Boolean,
    params: List<PType>
): Pair<List<PImport>, ParseState> = if (token?.value is ClosedParenToken) {
    val next = parser.pop()
    if (next?.value == AS_TOKEN) {
        val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
        listOf(
            (if (virtual) JavaVirtualMethodImport(
                path, params, (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name at area
            ) else JavaMethodImport(
                path, params, (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name at area
            )) at start..area.last
        ) to parser.pop()
    } else listOf(
        (if (virtual) JavaVirtualMethodImport(path, params, path.parts.last())
        else JavaMethodImport(path, params, path.parts.last())) at start..token.area.last
    ) to next
} else {
    val (type, symbol) = handleType(token, parser)
    when (symbol?.value) {
        is ClosedParenToken -> {
            val next = parser.pop()
            if (next?.value == AS_TOKEN) {
                val (alias, area) = parser.pop() ?: parser.error(INVALID_ALIAS_ERROR, parser.lastPos)
                listOf(
                    (if (virtual) JavaVirtualMethodImport(
                        path, params + type,
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name at area
                    ) else JavaMethodImport(
                        path, params + type,
                        (alias as? IdentifierToken ?: parser.error(INVALID_ALIAS_ERROR, area)).name at area
                    )) at start..area.last
                ) to parser.pop()
            } else listOf(
                (if (virtual) JavaVirtualMethodImport(path, params + type, path.parts.last())
                else JavaMethodImport(path, params + type, path.parts.last())) at start..symbol.area.last
            ) to next
        }
        is CommaToken -> handleJavaImportParams(parser.pop(), parser, start, path, virtual, params + type)
        else -> INVALID_IMPORT_ERROR throwAt state.pos
    }
}

tailrec fun handleJavaImportGroup(
    state: ParseState,
    start: StringPos,
    path: List<PString>,
    parts: List<PImport>,
    virtual: Boolean
): Pair<List<PImport>, ParseState> = if (state.char == '}') parts to state.next else {
    val (part, afterState) = handleJavaImport(state, start, path, virtual)
    val symbolState = afterState.actual
    when (symbolState.char) {
        '}' -> parts + part to symbolState.next
        ',' -> handleJavaImportGroup(symbolState.nextActual, start, path, parts + part, virtual)
        else -> UNCLOSED_IMPORT_GROUP_ERROR throwAt state.pos
    }
}